-- Fix MySQL user authentication for TCP/IP connections from Java
-- This script ensures the root user can connect via TCP/IP with password authentication

-- 1. Alter the root@localhost user to use mysql_native_password
ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY 'admin';

-- 2. Create root@127.0.0.1 user if it doesn't exist (for TCP/IP connections)
CREATE USER IF NOT EXISTS 'root'@'127.0.0.1' IDENTIFIED WITH mysql_native_password BY 'admin';

-- 3. Grant all privileges to both users
GRANT ALL PRIVILEGES ON *.* TO 'root'@'localhost' WITH GRANT OPTION;
GRANT ALL PRIVILEGES ON *.* TO 'root'@'127.0.0.1' WITH GRANT OPTION;

-- 4. Flush privileges to apply changes immediately
FLUSH PRIVILEGES;

-- Verify the configuration
SELECT user, host, plugin FROM mysql.user WHERE user='root';
