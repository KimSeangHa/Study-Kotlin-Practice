package com.group.libraryapp.service.book

import com.group.libraryapp.domain.book.Book
import com.group.libraryapp.domain.book.BookRepository
import com.group.libraryapp.domain.book.BookType
import com.group.libraryapp.domain.user.User
import com.group.libraryapp.domain.user.UserRepository
import com.group.libraryapp.domain.user.loanhistory.UserLoanHistory
import com.group.libraryapp.domain.user.loanhistory.UserLoanHistoryRepository
import com.group.libraryapp.domain.user.loanhistory.UserLoanStatus
import com.group.libraryapp.dto.book.request.BookLoanRequest
import com.group.libraryapp.dto.book.request.BookRequest
import com.group.libraryapp.dto.book.request.BookReturnRequest
import com.group.libraryapp.dto.book.response.BookStatResponse
import org.assertj.core.api.AssertionsForInterfaceTypes
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.lang.IllegalArgumentException

@SpringBootTest
class BookServiceTest @Autowired constructor(
    private val bookService: BookService,
    private val bookRepository: BookRepository,
    private val userRepository: UserRepository,
    private val userLoanHistoryRepository: UserLoanHistoryRepository,
) {

    @AfterEach
    fun clean() {
        bookRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    @DisplayName("책 등록이 정상 동작한다.")
    fun saveBookTest() {
        // given
        val request = BookRequest("노인과 바다", BookType.COMPUTER)

        // when
        bookService.saveBook(request)

        // then
        val books = bookRepository.findAll()
        AssertionsForInterfaceTypes.assertThat(books).hasSize(1)
        AssertionsForInterfaceTypes.assertThat(books[0].name).isEqualTo("노인과 바다")
        AssertionsForInterfaceTypes.assertThat(books[0].type).isEqualTo(BookType.COMPUTER)
    }

    @Test
    @DisplayName("책 대출이 정상 동작한다.")
    fun loanBookTest() {
        // given
        bookRepository.save(Book.fixture("노인과 바다"))
        val savedUser = userRepository.save(User("김승하", null))
        val request = BookLoanRequest("김승하", "노인과 바다")

        // when
        bookService.loanBook(request)

        // then
        val results = userLoanHistoryRepository.findAll()
        AssertionsForInterfaceTypes.assertThat(results).hasSize(1)
        AssertionsForInterfaceTypes.assertThat(results[0].bookName).isEqualTo("노인과 바다")
        AssertionsForInterfaceTypes.assertThat(results[0].user.id).isEqualTo(savedUser.id)
        AssertionsForInterfaceTypes.assertThat(results[0].status).isEqualTo(UserLoanStatus.LOANED)

    }

    @Test
    @DisplayName("책이 진작 대출되어 있다면, 신규 대출이 실패한다")
    fun loanBookFailTest() {
        // given
        bookRepository.save(Book.fixture("노인과 바다"))
        val savedUser = userRepository.save(User("김승하", null))
        userLoanHistoryRepository.save(UserLoanHistory.fixture(savedUser, "노인과 바다"))
        val request = BookLoanRequest("김승하", "노인과 바다")

        // when & then
        val message = assertThrows<IllegalArgumentException> {
            bookService.loanBook(request)
        }.message
        AssertionsForInterfaceTypes.assertThat(message).isEqualTo("진작 대출되어 있는 책입니다")
    }

    @Test
    @DisplayName("책 반납이 정상 동작한다.")
    fun returnBookTest() {
        // given
        val savedUser = userRepository.save(User("김승하", null))
        userLoanHistoryRepository.save(UserLoanHistory.fixture(savedUser, "노인과 바다"))
        val request = BookReturnRequest("김승하", "노인과 바다")

        // when
        bookService.returnBook(request)

        // then
        val results = userLoanHistoryRepository.findAll()
        AssertionsForInterfaceTypes.assertThat(results).hasSize(1)
        AssertionsForInterfaceTypes.assertThat(results[0].status).isEqualTo(UserLoanStatus.RETURNED)
    }

    @Test
    @DisplayName("책 대여 권수를 정상 확인한다.")
    fun countLoanedBookTest() {
        // given
        val savedUser = userRepository.save(User("김승하", null))
        userLoanHistoryRepository.saveAll(listOf(
            UserLoanHistory.fixture(savedUser, "A"),
            UserLoanHistory.fixture(savedUser, "B", UserLoanStatus.RETURNED),
            UserLoanHistory.fixture(savedUser, "C", UserLoanStatus.RETURNED),
        ))

        // when
        val result = bookService.countLoanedBook()

        // then
        AssertionsForInterfaceTypes.assertThat(result).isEqualTo(1)
    }

    @Test
    @DisplayName("분야 별 책 권수를 정상 확인한다.")
    fun getBookStatisticsTest() {
        // given
        bookRepository.saveAll(listOf(
            Book.fixture("A", BookType.COMPUTER),
            Book.fixture("B", BookType.COMPUTER),
            Book.fixture("C", BookType.SCIENCE),
        ))

        // when
        val results = bookService.getBookStatistics()

        // then
        AssertionsForInterfaceTypes.assertThat(results).hasSize(2)
        assertCount(results, BookType.COMPUTER, 2)
        assertCount(results, BookType.SCIENCE, 1L)

        val scienceDto = results.first { result -> result.type == BookType.SCIENCE }
        AssertionsForInterfaceTypes.assertThat(scienceDto.count).isEqualTo(1)
    }

    private fun assertCount(results: List<BookStatResponse>, type: BookType, count: Long) {
        AssertionsForInterfaceTypes.assertThat(results.first { result -> result.type == type }.count).isEqualTo(count)
    }
}