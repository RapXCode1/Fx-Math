package com.example.data.solver

import kotlin.math.*

object MathEngine {

    /**
     * Evaluate standard and scientific mathematical expression strings.
     */
    fun evaluate(expression: String, isRadians: Boolean = true): Double {
        val sanitized = expression
            .replace("π", "pi")
            .replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")
        return ExpressionParser(sanitized, isRadians).parse()
    }

    private class ExpressionParser(private val str: String, private val isRadians: Boolean) {
        private var pos = -1
        private var ch = 0

        private fun nextChar() {
            ch = if (++pos < str.length) str[pos].code else -1
        }

        private fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            while (ch == ' '.code) nextChar()
            if (pos < str.length) {
                throw IllegalArgumentException("Unexpected character: '${ch.toChar()}'")
            }
            return x
        }

        private fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                if (eat('+'.code)) x += parseTerm() // addition
                else if (eat('-'.code)) x -= parseTerm() // subtraction
                else break
            }
            return x
        }

        private fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                if (eat('*'.code)) x *= parseFactor() // multiplication
                else if (eat('/'.code)) {
                    val divisor = parseFactor()
                    if (divisor == 0.0) throw ArithmeticException("Division by zero")
                    x /= divisor // division
                } else if (eat('%'.code)) {
                    val divisor = parseFactor()
                    if (divisor == 0.0) throw ArithmeticException("Modulo by zero")
                    x %= divisor // modulo
                } else break
            }
            return x
        }

        private fun parseFactor(): Double {
            if (eat('+'.code)) return parseFactor() // unary plus
            if (eat('-'.code)) return -parseFactor() // unary minus

            var x: Double
            val startPos = this.pos
            if (eat('('.code)) { // parentheses
                x = parseExpression()
                eat(')'.code)
            } else if (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) { // numbers
                while (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) nextChar()
                x = str.substring(startPos, this.pos).toDouble()
            } else if (ch >= 'a'.code && ch <= 'z'.code) { // functions or constants
                while (ch >= 'a'.code && ch <= 'z'.code) nextChar()
                val func = str.substring(startPos, this.pos)
                if (func == "pi") {
                    x = Math.PI
                } else if (func == "e") {
                    x = Math.E
                } else {
                    x = parseFactor()
                    x = when (func) {
                        "sqrt" -> sqrt(x)
                        "cbrt" -> Math.cbrt(x)
                        "sin" -> if (isRadians) sin(x) else sin(Math.toRadians(x))
                        "cos" -> if (isRadians) cos(x) else cos(Math.toRadians(x))
                        "tan" -> if (isRadians) tan(x) else tan(Math.toRadians(x))
                        "asin" -> if (isRadians) asin(x) else Math.toDegrees(asin(x))
                        "acos" -> if (isRadians) acos(x) else Math.toDegrees(acos(x))
                        "atan" -> if (isRadians) atan(x) else Math.toDegrees(atan(x))
                        "sinh" -> sinh(x)
                        "cosh" -> cosh(x)
                        "tanh" -> tanh(x)
                        "log" -> log10(x)
                        "ln" -> ln(x)
                        "abs" -> abs(x)
                        "fact" -> factorial(x.toInt()).toDouble()
                        else -> throw IllegalArgumentException("Unknown function: '$func'")
                    }
                }
            } else {
                throw IllegalArgumentException("Unexpected character: '${ch.toChar()}'")
            }

            if (eat('^'.code)) x = x.pow(parseFactor()) // exponentiation

            return x
        }

        private fun factorial(n: Int): Long {
            if (n < 0) throw IllegalArgumentException("Factorial of negative number is undefined")
            var result = 1L
            for (i in 2..n) {
                result *= i
            }
            return result
        }
    }

    /**
     * Solves quadratic equation: a*x^2 + b*x + c = 0
     * Returns step by step derivation and roots.
     */
    fun solveQuadratic(a: Double, b: Double, c: Double): QuadraticSolution {
        if (a == 0.0) {
            if (b == 0.0) {
                return QuadraticSolution(
                    roots = emptyList(),
                    steps = listOf("Equation is $c = 0. This equation has no solutions.")
                )
            }
            val root = -c / b
            return QuadraticSolution(
                roots = listOf(root),
                steps = listOf(
                    "Linear Equation: ${b}x + $c = 0",
                    "Isolate term: ${b}x = -$c",
                    "Solve for x: x = -$c / $b = $root"
                )
            )
        }

        val disc = b * b - 4 * a * c
        val steps = mutableListOf<String>()
        steps.add("Equation: ${a}x² + (${b})x + (${c}) = 0")
        steps.add("Standard Formula: x = [-b ± √(b² - 4ac)] / 2a")
        steps.add("Calculate Discriminant (Δ): b² - 4ac")
        steps.add("Δ = (${b})² - 4*($a)*($c) = ${b*b} - ${4*a*c} = $disc")

        return when {
            disc > 0 -> {
                val r1 = (-b + sqrt(disc)) / (2 * a)
                val r2 = (-b - sqrt(disc)) / (2 * a)
                steps.add("Δ > 0: Two real distinct roots exist.")
                steps.add("x₁ = [-($b) + √($disc)] / (2*$a) = [${-b} + ${sqrt(disc)}] / ${2*a} = $r1")
                steps.add("x₂ = [-($b) - √($disc)] / (2*$a) = [${-b} - ${sqrt(disc)}] / ${2*a} = $r2")
                QuadraticSolution(listOf(r1, r2), steps, disc, isReal = true)
            }
            disc == 0.0 -> {
                val r = -b / (2 * a)
                steps.add("Δ = 0: One real double root exists.")
                steps.add("x = -($b) / (2*$a) = $r")
                QuadraticSolution(listOf(r), steps, disc, isReal = true)
            }
            else -> {
                // Complex roots
                val real = -b / (2 * a)
                val imag = sqrt(-disc) / (2 * a)
                steps.add("Δ < 0: Complex conjugate roots exist.")
                steps.add("x₁ = $real + ${imag}i")
                steps.add("x₂ = $real - ${imag}i")
                QuadraticSolution(
                    roots = emptyList(), // real roots empty
                    steps = steps,
                    discriminant = disc,
                    isReal = false,
                    complexRoots = Pair("$real + ${imag}i", "$real - ${imag}i")
                )
            }
        }
    }

    data class QuadraticSolution(
        val roots: List<Double>,
        val steps: List<String>,
        val discriminant: Double = 0.0,
        val isReal: Boolean = true,
        val complexRoots: Pair<String, String>? = null
    )

    /**
     * Matrix algebra calculator
     */
    fun determinant2x2(matrix: Array<DoubleArray>): Double {
        if (matrix.size != 2 || matrix[0].size != 2) throw IllegalArgumentException("Matrix must be 2x2")
        return matrix[0][0] * matrix[1][1] - matrix[0][1] * matrix[1][0]
    }

    fun transpose(matrix: Array<DoubleArray>): Array<DoubleArray> {
        val rows = matrix.size
        val cols = matrix[0].size
        val result = Array(cols) { DoubleArray(rows) }
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                result[j][i] = matrix[i][j]
            }
        }
        return result
    }

    /**
     * Units converter
     */
    fun convertUnit(value: Double, from: String, to: String, category: String): Double {
        return when (category.lowercase()) {
            "length" -> {
                val meters = when (from.lowercase()) {
                    "m", "meter" -> value
                    "km", "kilometer" -> value * 1000.0
                    "cm", "centimeter" -> value * 0.01
                    "mm", "millimeter" -> value * 0.001
                    "inch", "in" -> value * 0.0254
                    "feet", "ft" -> value * 0.3048
                    "mile", "mi" -> value * 1609.34
                    else -> value
                }
                when (to.lowercase()) {
                    "m", "meter" -> meters
                    "km", "kilometer" -> meters / 1000.0
                    "cm", "centimeter" -> meters / 0.01
                    "mm", "millimeter" -> meters / 0.001
                    "inch", "in" -> meters / 0.0254
                    "feet", "ft" -> meters / 0.3048
                    "mile", "mi" -> meters / 1609.34
                    else -> meters
                }
            }
            "weight" -> {
                val grams = when (from.lowercase()) {
                    "g", "gram" -> value
                    "kg", "kilogram" -> value * 1000.0
                    "mg", "milligram" -> value * 0.001
                    "lb", "pound" -> value * 453.59237
                    "oz", "ounce" -> value * 28.34952
                    else -> value
                }
                when (to.lowercase()) {
                    "g", "gram" -> grams
                    "kg", "kilogram" -> grams / 1000.0
                    "mg", "milligram" -> grams / 0.001
                    "lb", "pound" -> grams / 453.59237
                    "oz", "ounce" -> grams / 28.34952
                    else -> grams
                }
            }
            "temperature" -> {
                val celsius = when (from.uppercase()) {
                    "C", "CELSIUS" -> value
                    "F", "FAHRENHEIT" -> (value - 32) * 5 / 9
                    "K", "KELVIN" -> value - 273.15
                    else -> value
                }
                when (to.uppercase()) {
                    "C", "CELSIUS" -> celsius
                    "F", "FAHRENHEIT" -> celsius * 9 / 5 + 32
                    "K", "KELVIN" -> celsius + 273.15
                    else -> celsius
                }
            }
            else -> value
        }
    }
}
