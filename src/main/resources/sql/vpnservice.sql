DROP TABLE IF EXISTS "l_vpnservice";
CREATE TABLE "l_vpnservice"
(
    "key_1"       TEXT NOT NULL PRIMARY KEY,
    "value_1"     TEXT NOT NULL,
    "description" TEXT,
    "create_time" DATETIME,
    "update_time" DATETIME
);