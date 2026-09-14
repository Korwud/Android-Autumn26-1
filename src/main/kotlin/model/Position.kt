package model

// Задание: добавьте константы позиций из CSV (FORWARD, MIDFIELD, DEFENDER, GOALKEEPER)
// с русским названием у каждой, а также companion-функцию
// fromCsvTokenOrNull(token: String): Position?, не зависящую от регистра и лишних пробелов.
enum class Position(val position: String) {
    FORWARD("Нападающий"),
    MIDFIELD("Полузащитник"),
    DEFENDER("Защитник"),
    GOALKEEPER("Вратарь");

    companion object {
        fun fromCsvTokenOrNull(token: String): Position? {
            val normalized: String = token.trim().uppercase();
            return entries.firstOrNull {it.name == normalized};
        }
    }
}
