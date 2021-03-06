/*
 *  # Trove
 *
 *  This file is part of Trove - A FREE desktop budgeting application that
 *  helps you track your finances, FREES you from complex budgeting, and
 *  enables you to build your TROVE of savings!
 *
 *  Copyright © 2016-2021 Eric John Fredericks.
 *
 *  Trove is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Trove is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Trove.  If not, see <http://www.gnu.org/licenses/>.
 */

package trove.core.infrastructure.persist.lock

import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import trove.exceptional.SystemException

import java.io.{File, IOException, RandomAccessFile}
import java.nio.channels.FileLock
import java.nio.file.Files
import scala.util.{Failure, Success, Try}

class ProjectLockSpec extends AnyFlatSpec with MockitoSugar with BeforeAndAfterEach with should.Matchers {

  import ProjectLock._

  val userHome: String = System.getProperty("user.home")
  val projectName = "unittest"
  val tempDir: File = Files.createTempDirectory("temp").toFile
  val actualFile = new File(tempDir, constructLockfileName(projectName))
  val separator: String = File.separator
  val expectedDirectory: File = tempDir
  val expectedFilename = s"$projectName${ProjectLock.LockfileSuffix}"

  val UnitSuccess: Success[Unit] = Success(())

  override def afterEach(): Unit = {
    if(actualFile.exists()) {
      actualFile.deleteOnExit()
    }
    if(tempDir.exists()) {
      tempDir.deleteOnExit()
    }
  }

  trait Fixture {

    var filesCreated: List[(File, String)] = List.empty
    val mockFile: File = mock[File]

    var randomAccessFilesCreated: List[File] = List.empty
    val mockRandomAccessFile: RandomAccessFile = mock[RandomAccessFile]

    var channelsCreated: List[RandomAccessFile] = List.empty
    val mockChannel: LockableChannel = mock[LockableChannel]
    var channelTryLockReturnsNull = false

    val mockFileLock: FileLock = mock[FileLock]

    var handleErrorArgs: List[Try[Unit]] = List.empty

    val throwExceptionOnCreateRandomAccessFile = false

    val projectLock: ProjectLock = new ProjectLock(tempDir, projectName) with EnvironmentOps {

      override def newFile(directory: File, filename: String): File = {
        filesCreated = (directory, filename) +: filesCreated
        mockFile
      }

      override def newRandomAccessFile(file: File): RandomAccessFile = if(throwExceptionOnCreateRandomAccessFile) {
        throw new RuntimeException("doom")
      }
      else {
        randomAccessFilesCreated = file +: randomAccessFilesCreated
        mockRandomAccessFile
      }

      override def newChannel(raf: RandomAccessFile): LockableChannel = {
        channelsCreated = raf +: channelsCreated
        if(channelTryLockReturnsNull) when(mockChannel.tryLock()).thenReturn(null)
        mockChannel
      }

      override def handleLockResourceReleaseError(result: Try[Unit]): Unit = handleErrorArgs = result +: handleErrorArgs
    }
  }

  "ProjectLock" should "allocate resources when lock is called" in new Fixture {
    when(mockChannel.tryLock()).thenReturn(mockFileLock)
    val result: Try[Unit] = projectLock.lock()
    result.isSuccess shouldBe true

    filesCreated should contain theSameElementsAs List((expectedDirectory, expectedFilename))
    randomAccessFilesCreated should contain theSameElementsAs List(mockFile)
    channelsCreated should contain theSameElementsAs List(mockRandomAccessFile)

    verify(mockChannel, times(1)).tryLock()

    verify(mockFileLock, never).release()
    verify(mockFileLock, never).close()
    verify(mockChannel, never).close()
    verify(mockFile, never).delete()

    handleErrorArgs shouldBe empty
}

  it should "return SystemError and not allocate resources if it cannot acquire lock (tryLock returns null)" in new Fixture {
    channelTryLockReturnsNull = true

    val result: Try[Unit] = projectLock.lock()
    result match {
      case Failure(_: SystemException) => // no op
      case _ => fail("wrong result when locking")
    }

    filesCreated should contain theSameElementsAs List((expectedDirectory, expectedFilename))
    randomAccessFilesCreated should contain theSameElementsAs List(mockFile)
    channelsCreated should contain theSameElementsAs List(mockRandomAccessFile)

    verify(mockChannel, times(1)).tryLock()

    verify(mockFileLock, never).release()
    verify(mockFileLock, never).close()

    verify(mockChannel, times(1)).close()
    verify(mockFile, never).delete()

    handleErrorArgs should not be empty
    val failures: List[Try[Unit]] = handleErrorArgs.filter(_.isFailure)
    failures shouldBe empty
  }

  it should "return SystemError and not allocate resources if exception is thrown while creating RandomAccessFIle" in new Fixture {
    override val throwExceptionOnCreateRandomAccessFile: Boolean = true
    val result: Try[Unit] = projectLock.lock()
    result match {
      case Failure(_: SystemException) => // no op
      case _ => fail("wrong result when locking")
    }

    filesCreated should contain theSameElementsAs List((expectedDirectory, expectedFilename))
    randomAccessFilesCreated shouldBe empty
    channelsCreated shouldBe empty

    verify(mockChannel, never).tryLock()

    verify(mockFileLock, never).release()
    verify(mockFileLock, never).close()

    verify(mockChannel, never).close()
    verify(mockFile, never).delete()

    handleErrorArgs shouldBe empty
  }

  it should "return SystemError, close the channel, and not allocate resources if an exception is thrown while it is trying to acquire lock" in new Fixture {

    when(mockChannel.tryLock()).thenThrow(new IOException("doom"))
    val result: Try[Unit] = projectLock.lock()
    result match {
      case Failure(_: SystemException) => // no op
      case _ => fail("wrong result when locking")
    }

    filesCreated should contain theSameElementsAs List((expectedDirectory, expectedFilename))
    randomAccessFilesCreated should contain theSameElementsAs List(mockFile)
    channelsCreated should contain theSameElementsAs List(mockRandomAccessFile)

    verify(mockChannel, times(1)).tryLock()

    verify(mockFileLock, never).release()
    verify(mockFileLock, never).close()

    verify(mockChannel, times(1)).close()
    verify(mockFile, never).delete()

    handleErrorArgs should not be empty
    handleErrorArgs.filter(_.isFailure) shouldBe empty
  }


  it should "release resources and delete file when release is called" in new Fixture {
    when(mockChannel.tryLock()).thenReturn(mockFileLock)
    val result: Try[Unit] = projectLock.lock()
    result.isSuccess shouldBe true
    projectLock.release()
    projectLock.isLocked shouldBe false

    verify(mockChannel, times(1)).tryLock()
    verify(mockFileLock, times(1)).release()
    verify(mockFileLock, times(1)).close()
    verify(mockChannel, times(1)).close()
    verify(mockFile, times(1)).delete()

    handleErrorArgs should not be empty
    handleErrorArgs.filter(_.isFailure) shouldBe empty
  }

  it should "be capable of re-locking" in new Fixture {
    when(mockChannel.tryLock()).thenReturn(mockFileLock)
    val result: Try[Unit] = projectLock.lock()
    result.isSuccess shouldBe true
    projectLock.release()
    projectLock.isLocked shouldBe false

    // Re-lock
    projectLock.lock() shouldBe Success(())
    projectLock.release()

    verify(mockChannel, times(2)).tryLock()
    verify(mockFileLock, times(2)).release()
    verify(mockFileLock, times(2)).close()
    verify(mockChannel, times(2)).close()
    verify(mockFile, times(2)).delete()
  }

  it should "cleanup all other resources when exception is thrown during file lock release when lock owner releases lock" in new Fixture {
    when(mockChannel.tryLock()).thenReturn(mockFileLock)
    doThrow(new IOException("doom")).when(mockFileLock).release()

    val result: Try[Unit] = projectLock.lock()
    result.isSuccess shouldBe true
    projectLock.release()
    projectLock.isLocked shouldBe true
    verify(mockFileLock, times(1)).release()
    verify(mockChannel, times(1)).close()
    verify(mockFile, times(1)).delete()

    handleErrorArgs should not be empty
    val failures: List[Try[Unit]] = handleErrorArgs.filter(_.isFailure)
    failures.size shouldBe 1
    failures.head.isFailure shouldBe true
    failures.head match {
      case Failure(_: IOException) => // no op
      case a: Any => fail(s"Wrong handled result")
    }
  }

  it should "cleanup all other resources, return failure, and handle errors when exception is thrown when closing channel" in new Fixture {
    when(mockChannel.tryLock()).thenReturn(mockFileLock)
    doThrow(new RuntimeException("doom")).when(mockChannel).close()

    val result: Try[Unit] = projectLock.lock()
    result.isSuccess shouldBe true
    projectLock.release()
    projectLock.isLocked shouldBe false

    verify(mockFileLock, times(1)).close()
    verify(mockChannel, times(1)).close()
    verify(mockFile, times(1)).delete()

    handleErrorArgs should not be empty
    val failures: List[Try[Unit]] = handleErrorArgs.filter(_.isFailure)
    failures.size shouldBe 1
    failures.head match {
      case Failure(_: RuntimeException) => // no op
      case a: Any => fail(s"Wrong handled result: $a")
    }
  }

  it should "cleanup all other resources, return success, and handle errors when exception is thrown deleting file" in new Fixture {
    when(mockChannel.tryLock()).thenReturn(mockFileLock)
    doThrow(new RuntimeException("doom")).when(mockFile).delete()

    val result: Try[Unit] = projectLock.lock()
    result.isSuccess shouldBe true
    projectLock.release()
    projectLock.isLocked shouldBe false

    verify(mockFileLock, times(1)).close()
    verify(mockFileLock, times(1)).close()
    verify(mockChannel, times(1)).close()
    verify(mockFile, times(1)).delete()

    handleErrorArgs should not be empty
    val failures: List[Try[Unit]] = handleErrorArgs.filter(_.isFailure)
    failures.size shouldBe 1
    failures.head match {
      case Failure(_: RuntimeException) => // no op
      case a: Any => fail(s"Wrong handled result: $a")
    }
  }
}
