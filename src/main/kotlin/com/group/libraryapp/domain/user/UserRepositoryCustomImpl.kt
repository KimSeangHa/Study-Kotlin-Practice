package com.group.libraryapp.domain.user

import com.querydsl.jpa.impl.JPAQueryFactory
import com.group.libraryapp.domain.user.QUser.user
import com.group.libraryapp.domain.user.loanhistory.QUserLoanHistory.userLoanHistory

class UserRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory,
) : UserRepositoryCustom {

    override fun findAllWithHistories(): List<User> {
        return queryFactory.select(user).distinct()
            .from(user)
            .leftJoin(userLoanHistory).on(userLoanHistory.user.id.eq(user.id)).fetchJoin()
            .fetch()
    }
}