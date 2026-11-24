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
@Table(name = "venues")
class VenueEntity(
    @Column(nullable = false, length = NAME_MAX_LENGTH)
    val name: String,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L

    init {
        requireRule(name.isNotBlank() && name.length <= NAME_MAX_LENGTH) { "공연장 이름은 1~${NAME_MAX_LENGTH}자여야 합니다." }
    }

    companion object {
        const val NAME_MAX_LENGTH = 100
    }
}
