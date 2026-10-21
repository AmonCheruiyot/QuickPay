const Database = require('better-sqlite3');
const config = require('../config/env');
const { runMigrations } = require('./migrations');

let dbInstance = null;

function getDb(customPath) {
  if (dbInstance) {
    return dbInstance;
  }

  const dbPath = customPath || config.dbPath;
  dbInstance = new Database(dbPath);
  dbInstance.pragma('journal_mode = WAL');
  dbInstance.pragma('foreign_keys = ON');

  runMigrations(dbInstance);

  return dbInstance;
}

function closeDb() {
  if (dbInstance) {
    dbInstance.close();
    dbInstance = null;
  }
}

module.exports = {
  getDb,
  closeDb,
};
