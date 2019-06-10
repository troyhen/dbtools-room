@file:Suppress("MemberVisibilityCanBePrivate")

const val KOTLIN_VERSION = "1.3.31"

object AndroidSdk {
    const val MIN = 21
    const val COMPILE = 28
    const val TARGET = COMPILE
}

object Pom {
    const val GROUP_ID = "org.dbtools"
    const val VERSION_NAME = "4.9.3"
    const val POM_DESCRIPTION = "DBTools Room"

    const val URL = "https://github.com/jeffdcamp/dbtools-room/"
    const val SCM_URL = "https://github.com/jeffdcamp/dbtools-room/"
    const val SCM_CONNECTION = "scm:git:git://github.com/jeffdcamp/dbtools-room.git"
    const val SCM_DEV_CONNECTION = "scm:git:git@github.com:jeffdcamp/dbtools-room.git"

    const val LICENCE_NAME = "The Apache Software License, Version 2.0"
    const val LICENCE_URL = "http://www.apache.org/licenses/LICENSE-2.0.txt"
    const val LICENCE_DIST = "repo"

    const val DEVELOPER_ID = "jcampbell"
    const val DEVELOPER_NAME = "Jeff Campbell"

    const val LIBRARY_ARTIFACT_ID = "dbtools-room"
    const val LIBRARY_JDBC_ARTIFACT_ID = "dbtools-room-jdbc"
    const val LIBRARY_SQLITE_ORG_ARTIFACT_ID = "dbtools-room-sqliteorg"
}

