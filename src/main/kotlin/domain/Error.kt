package domain

sealed class Error(val message: String) {
}

class ParseError(message: String) : Error(message)