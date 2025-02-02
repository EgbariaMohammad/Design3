package il.ac.technion.cs.sd.buy.app

interface IParser<T> {
    fun parse(data: String) : List<T>
}