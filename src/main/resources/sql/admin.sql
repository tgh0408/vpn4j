DROP TABLE IF EXISTS "l_admin";
CREATE TABLE "l_admin"
(
    "id"          INTEGER PRIMARY KEY AUTOINCREMENT,
    "account"     TEXT     NOT NULL,
    "avatar"      TEXT,
    "email"       TEXT     NOT NULL,
    "password"    TEXT     NOT NULL,
    "nickname"    TEXT     NOT NULL,
    "del_flag"    TEXT     NOT NULL DEFAULT '0',
    "is_admin"    TEXT     NOT NULL DEFAULT '0',
    "sex"         TEXT     NOT NULL DEFAULT -1,
    "phone"       TEXT,
    "address"     TEXT,
    "description" TEXT,
    "create_time" DATETIME NOT NULL,
    "update_time" DATETIME,
    "user_type"   TEXT              DEFAULT 'user',
    "source"      TEXT     NOT NULL DEFAULT 'SQLITE'
);

INSERT INTO "l_admin" ("id", "account", "email", "password", "nickname", "del_flag", "is_admin", "sex",
                       "phone", "address", "description",
                       "create_time", "update_time", "user_type", "source")
VALUES (1,
        'admin',
        '',
        'admin',
        '管理员',
        '0',
        '1',
        '-1',
        null,
        null,
        'OpenVPN 管理系统',
        datetime('now', 'localtime'),
        null,
        'admin',
        'SQLITE');