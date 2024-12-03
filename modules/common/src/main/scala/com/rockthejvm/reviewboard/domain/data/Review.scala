package com.rockthejvm.reviewboard.domain.data

import java.time.Instant

import zio.json.JsonCodec
import zio.json.DeriveJsonCodec


final case class Review(
    id: Long, // PK
    companyId: Long,
    userId: Long, // FK
    management: Int, // 1-5
    culture: Int,
    salary: Int,
    benefits: Int,
    wouldRecommend: Int,
    review: String,
    created: Instant, // java time type works well with jdbc
    updated: Instant
)

object Review {
    given codec: JsonCodec[Review] = DeriveJsonCodec.gen[Review]

    def empty(companyId: Long) = Review(
        -1L,
        companyId,
        -1L,
        5,5,5,5,5,
        "",
        Instant.now(),
        Instant.now()
    )
}

// ***IMP : WE WRITE code for REVIEW USING (just so we know different ways to implement things):
    // bottom-up approach: repository -> service -> http
    // TDD (we write Tests first)
