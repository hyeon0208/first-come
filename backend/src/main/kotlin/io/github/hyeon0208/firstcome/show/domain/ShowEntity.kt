package io.github.hyeon0208.firstcome.show.domain

import io.github.hyeon0208.firstcome.support.domain.BaseEntity
import io.github.hyeon0208.firstcome.support.error.requireRule
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "shows")
class ShowEntity(
    @Column(nullable = false)
    val venueId: Long,

    @Column(nullable = false, length = TITLE_MAX_LENGTH)
    val title: String,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L

    init {
        requireRule(title.isNotBlank() && title.length <= TITLE_MAX_LENGTH) { "공연 제목은 1~${TITLE_MAX_LENGTH}자여야 합니다." }
    }

    companion object {
        const val TITLE_MAX_LENGTH = 100
    }
}
