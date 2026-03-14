DROP TABLE IF EXISTS "l_black";
CREATE TABLE "l_black" (
                           "id" INTEGER PRIMARY KEY AUTOINCREMENT,
                           "type" TEXT NOT NULL DEFAULT 'username',
                           "data_1" TEXT,
                           "duration" INTEGER NOT NULL DEFAULT 0,
                           "release_time" DATETIME NOT NULL,
                           "notes" TEXT,
                           "create_time" DATETIME
);
CREATE INDEX "idx_l_black_type_data" ON "l_black" ("type", "data_1");

