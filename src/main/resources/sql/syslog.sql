DROP TABLE IF EXISTS "l_syslog";
CREATE TABLE "l_syslog" (
                            "oper_id" INTEGER PRIMARY KEY AUTOINCREMENT,
                            "title" TEXT DEFAULT '',
                            "business_type" INTEGER DEFAULT 0,
                            "method" TEXT DEFAULT '',
                            "request_method" TEXT DEFAULT '',
                            "operator_type" INTEGER DEFAULT 0,
                            "oper_name" TEXT DEFAULT '',
                            "oper_url" TEXT DEFAULT '',
                            "oper_ip" TEXT DEFAULT '',
                            "oper_location" TEXT DEFAULT '',
                            "oper_param" TEXT,
                            "json_result" TEXT,
                            "status" INTEGER DEFAULT 0,
                            "error_msg" TEXT DEFAULT '',
                            "oper_time" DATETIME,
                            "cost_time" INTEGER DEFAULT 0
);
CREATE INDEX "idx_oper_name" ON "l_syslog" ("oper_name");
CREATE INDEX "idx_oper_time" ON "l_syslog" ("oper_time");
CREATE INDEX "idx_title" ON "l_syslog" ("title");
CREATE INDEX "idx_status" ON "l_syslog" ("status");