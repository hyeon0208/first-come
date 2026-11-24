package io.github.hyeon0208.firstcome.show.domain

import io.github.hyeon0208.firstcome.support.domain.BaseEntity
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

    @Column(nullable = false, length = 100)
    val title: String,

    @Column(nullable = false)
    val runningMinutes: Int,

    @Column(nullable = false, length = 30)
    val ageLimit: String,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L
}
