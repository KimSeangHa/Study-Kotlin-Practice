package com.group.libraryapp.service.book

import com.group.libraryapp.domain.book.Book
import com.group.libraryapp.domain.book.BookRepository
import com.group.libraryapp.domain.user.User
import com.group.libraryapp.domain.user.UserRepository
import com.group.libraryapp.domain.user.loanhistory.UserLoanHistory
import com.group.libraryapp.domain.user.loanhistory.UserLoanHistoryRepository
import com.group.libraryapp.dto.book.request.BookLoanRequest
import com.group.libraryapp.dto.book.request.BookRequest
import com.group.libraryapp.dto.book.request.BookReturnRequest
import org.assertj.core.api.AssertionsForInterfaceTypes
import org.junit.jupiter.api.AfterEach
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
        val request = BookRequest("노인과 바다")

        // when
        bookService.saveBook(request)

        // then
        val books = bookRepository.findAll()
        AssertionsForInterfaceTypes.assertThat(books).hasSize(1)
        AssertionsForInterfaceTypes.assertThat(books[0].name).isEqualTo("노인과 바다")
    }

    @Test
    @DisplayName("책 대출이 정상 동작한다.")
    fun loanBookTest() {
        // given
        bookRepository.save(Book("노인과 바다"))
        val savedUser = userRepository.save(User("김승하", null))
        val request = BookLoanRequest("김승하", "노인과 바다")

        // when
        bookService.loanBook(request)

        // then
        val results = userLoanHistoryRepository.findAll()
        AssertionsForInterfaceTypes.assertThat(results).hasSize(1)
        AssertionsForInterfaceTypes.assertThat(results[0].bookName).isEqualTo("노인과 바다")
        AssertionsForInterfaceTypes.assertThat(results[0].user.id).isEqualTo(savedUser.id)
        AssertionsForInterfaceTypes.assertThat(results[0].isReturn).isFalse

    }

    @Test
    @DisplayName("책이 진작 대출되어 있다면, 신규 대출이 실패한다")
    fun loanBookFailTest() {
        // given
        bookRepository.save(Book("노인과 바다"))
        val savedUser = userRepository.save(User("김승하", null))
        userLoanHistoryRepository.save(UserLoanHistory(savedUser, "노인과 바다", false))
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
        userLoanHistoryRepository.save(UserLoanHistory(savedUser, "노인과 바다", false))
        val request = BookReturnRequest("김승하", "노인과 바다")

        // when
        bookService.returnBook(request)

        // then
        val results = userLoanHistoryRepository.findAll()
        AssertionsForInterfaceTypes.assertThat(results).hasSize(1)
        AssertionsForInterfaceTypes.assertThat(results[0].isReturn).isTrue
    }
}