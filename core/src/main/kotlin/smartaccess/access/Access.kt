package smartaccess.access

import smartaccess.annotation.ProvideAccessTemple
import rain.api.annotation.LoadBy

@ProvideAccessTemple
@LoadBy(AccessLoader::class, mastBean = false)
interface Access<T, PK>{

    val modelType: Class<T>
    val primaryKeyType: Class<PK>

}