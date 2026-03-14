DROP TABLE IF EXISTS "l_ccd";
CREATE TABLE "l_ccd" (
                         "id" INTEGER PRIMARY KEY AUTOINCREMENT,
                         "username" TEXT NOT NULL,
                         "ccd_config" TEXT,
                         "create_time" DATETIME,
                         "update_time" DATETIME,
                         "status" INTEGER
);