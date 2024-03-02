package com.IceCreamQAQ.SmartAccess.db.transaction

import com.IceCreamQAQ.SmartAccess.annotation.Transactional
import rain.hook.HookContext
import rain.hook.HookInfo
import rain.hook.HookRunnable
import rain.hook.InstanceMode

@InstanceMode
class TransactionHook(private val ts: TransactionService) : HookRunnable {

    override fun init(info: HookInfo) {
        val dbList = info.method.getAnnotation(Transactional::class.java)?.dbList ?: arrayOf("default")
        info.saveInfo["SmartAccess.dbList"] = dbList
    }

    override fun preRun(context: HookContext): Boolean {
        val databases = context.info.saveInfo["SmartAccess.dbList"] as Array<String>

        ts.beginTransactionSync(databases)?.let {
            context.saveInfo("SmartAccess.Transaction", it)
        }
        return false
    }

    override fun postRun(context: HookContext) {
        val transaction = context.getInfo("SmartAccess.Transaction") as? TransactionContext ?: return
        try {
            transaction.commitSync()
        } catch (e: Throwable) {
            transaction.rollbackSync()
        }
    }

    override fun onError(context: HookContext): Boolean {
        (context.getInfo("SmartAccess.Transaction") as? TransactionContext ?: return false).rollbackSync()
        return false
    }

}