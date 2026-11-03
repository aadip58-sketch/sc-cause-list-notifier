package in.gov.sci.causelist.model

data class CauseListItem(
    val id: String,
    val date: String,
    val title: String,
    val court: String,
    val category: String,
    val pdfUrl: String,
    val discoveredAt: Long = System.currentTimeMillis()
)
