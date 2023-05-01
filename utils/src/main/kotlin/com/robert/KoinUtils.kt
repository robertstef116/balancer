package com.robert

import org.koin.core.component.KoinComponent
import org.koin.core.component.KoinScopeComponent
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier
import kotlin.reflect.KClass

inline fun <reified T : Any> KoinComponent.get(
    clazz: KClass<*>,
    qualifier: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null
): T {
    return if (this is KoinScopeComponent) {
        scope.get(clazz, qualifier, parameters)
    } else getKoin().get(clazz, qualifier, parameters)
}
