DROP TABLE IF EXISTS "l_account";
CREATE TABLE "l_account" (
                             "id" INTEGER PRIMARY KEY AUTOINCREMENT,
                             "nickname" TEXT NOT NULL,
                             "username" TEXT NOT NULL,
                             "password" TEXT NOT NULL,
                             "client_crt" TEXT,
                             "client_key" TEXT,
                             "email" TEXT NOT NULL,
                             "online" TEXT NOT NULL DEFAULT '0',
                             "status" TEXT NOT NULL DEFAULT '1',
                             "start_time" DATETIME,
                             "expire_time" DATETIME,
                             "static_ip" TEXT,
                             "create_time" DATETIME,
                             "update_time" DATETIME
);