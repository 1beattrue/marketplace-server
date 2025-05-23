package ru.mirea

import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class MarketItem(
    val title: String,
    val description: String,
    val price: Int,
    val discountPercentage: Double,
    val rating: Double,
    val stock: Int,
    val brand: String,
    val category: String,
    val thumbnail: String
)

class MarketItemService(database: Database) {
    object MarketItems : Table() {
        val id = integer("id").autoIncrement()
        val title = varchar("title", length = 255)
        val description = text("description")
        val price = integer("price")
        val discountPercentage = double("discountPercentage")
        val rating = double("rating")
        val stock = integer("stock")
        val brand = varchar("brand", length = 255)
        val category = varchar("category", length = 255)
        val thumbnail = text("thumbnail")

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.create(MarketItems)
        }
    }

    suspend fun create(item: MarketItem): Int = dbQuery {
        MarketItems.insert {
            it[title] = item.title
            it[description] = item.description
            it[price] = item.price
            it[discountPercentage] = item.discountPercentage
            it[rating] = item.rating
            it[stock] = item.stock
            it[brand] = item.brand
            it[category] = item.category
            it[thumbnail] = item.thumbnail
        }[MarketItems.id]
    }

    suspend fun read(id: Int): MarketItem? = dbQuery {
        MarketItems.selectAll()
            .where { MarketItems.id eq id }
            .map {
                MarketItem(
                    title = it[MarketItems.title],
                    description = it[MarketItems.description],
                    price = it[MarketItems.price],
                    discountPercentage = it[MarketItems.discountPercentage],
                    rating = it[MarketItems.rating],
                    stock = it[MarketItems.stock],
                    brand = it[MarketItems.brand],
                    category = it[MarketItems.category],
                    thumbnail = it[MarketItems.thumbnail],
                )
            }
            .singleOrNull()
    }

    suspend fun update(id: Int, item: MarketItem) {
        dbQuery {
            MarketItems.update({ MarketItems.id eq id }) {
                it[title] = item.title
                it[description] = item.description
                it[price] = item.price
                it[discountPercentage] = item.discountPercentage
                it[rating] = item.rating
                it[stock] = item.stock
                it[brand] = item.brand
                it[category] = item.category
                it[thumbnail] = item.thumbnail
            }
        }
    }

    suspend fun delete(id: Int) {
        dbQuery {
            MarketItems.deleteWhere { MarketItems.id eq id }
        }
    }

    suspend fun getPaged(limit: Int, skip: Int) = dbQuery {
        MarketItems.selectAll()
            .drop(skip)
            .take(limit)
            .map { toMarketItem(it) }
    }

    suspend fun getAllCategories(): List<String> = dbQuery {
        MarketItems
            .select(MarketItems.category)
            .withDistinct()
            .map { it[MarketItems.category] }
    }

    suspend fun getByCategory(category: String) = dbQuery {
        MarketItems
            .selectAll()
            .where { MarketItems.category eq category }
            .map { toMarketItem(it) }
    }

    suspend fun search(q: String) = dbQuery {
        MarketItems
            .selectAll()
            .where { MarketItems.title.lowerCase() like "${q.lowercase()}%" }
            .map { toMarketItem(it) }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    private fun toMarketItem(row: ResultRow): MarketItem {
        return MarketItem(
            title = row[MarketItems.title],
            description = row[MarketItems.description],
            price = row[MarketItems.price],
            discountPercentage = row[MarketItems.discountPercentage],
            rating = row[MarketItems.rating],
            stock = row[MarketItems.stock],
            brand = row[MarketItems.brand],
            category = row[MarketItems.category],
            thumbnail = row[MarketItems.thumbnail],
        )
    }
}