//useDbConnection.js
import api from "@/api";

const testConnection = (db) => api.post('/api/db-connection/test', db)
export function useDbConnection() {
  const getDbList = () => api.get('/api/db-connection');
  const addDb = (db) => api.post('/api/db-connection', db);
  const updateDb = (id, db) => api.put(`/api/db-connection/${id}`, db);
  const deleteDb = (id) => api.delete(`/api/db-connection/${id}`);
  return { getDbList, addDb, updateDb, deleteDb, testConnection };
}



