
class FormattedStringBuffer(val indentString: String = "  ") {
    internal var indent = 0
    val sb = StringBuilder()

    fun indent() {
        indent++
    }

    fun deindent() {
        indent--
    }

    fun indented(block: () -> Unit) {
        indent()
        block()
        deindent()
    }

    fun appendIndentation() {
        for (i in 1 .. indent) {
            sb.append(indentString)
        }
    }

    fun appendln(any: Any) {
        appendIndentation()
        sb.append(any)
        sb.append("\n")
    }

    override fun toString() = sb.toString()
}

fun <T> printfRec(obj: T, fsb: FormattedStringBuffer = FormattedStringBuffer(),
                  doPrint: Boolean = true,
                  printer: (FormattedStringBuffer, T, (T) -> Unit) -> Unit) {

    printer(fsb, obj) { subObj ->
        printfRec(subObj, fsb, false, printer)
    }

    if (doPrint) println(fsb)
}