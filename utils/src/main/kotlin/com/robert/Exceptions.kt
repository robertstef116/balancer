package com.robert

class UndefinedVariableExceptions(variableName: String) : RuntimeException("key $variableName not set, make sure that the variable is set in the environment")
