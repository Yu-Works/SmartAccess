package com.IceCreamQAQ.SmartAccess.hibernate

import com.IceCreamQAQ.SmartAccess.jpa.JPAService
import com.IceCreamQAQ.Yu.annotation.Config
import com.IceCreamQAQ.Yu.util.dataNode.ObjectNode
import javax.inject.Named

@Named("hibernate5")
class HibernateService(@Config db: ObjectNode) : JPAService(db)