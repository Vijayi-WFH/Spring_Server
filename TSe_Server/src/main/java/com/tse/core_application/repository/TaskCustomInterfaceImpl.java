package com.tse.core_application.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tse.core_application.model.Task;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskCustomInterfaceImpl implements TaskCustomInterface{
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public boolean updateTask(Task task, List<String> fields) {

        //converting task to a hashmap , to iterate through all fields.
        ObjectMapper m = new ObjectMapper();
        HashMap<String,Object> taskHM = m.convertValue(task, HashMap.class);
        if(taskHM!=null){
            StringBuilder updateString = new StringBuilder("SET");
            int counterForAnd=0;
            for(Map.Entry taskHMElement:taskHM.entrySet()){
              if(fields.contains(taskHMElement.getKey())){
                  //appending to update string
                  updateString.append(" T.").append(taskHMElement.getKey()).append("=:").append(taskHMElement.getKey());
                  counterForAnd++;
                  if(counterForAnd<taskHM.entrySet().size()){
                      updateString.append(" and");
                  }
              }
            }
            //forming entire query. if we need filters in where clause, use above approach to create WhereString as well.
            String queryString = "UPDATE Task T "+ updateString+ "WHERE T.taskId="+task.getTaskId();
            Query query = entityManager.createQuery(queryString);


            //dynamically calling setParameters, so we can set values in dynamically generated update clause.
            for (Map.Entry mapElement : taskHM.entrySet()) {
                String key = (String)mapElement.getKey();
                query=query.setParameter(key,mapElement.getValue());
            }

            //then executing query:
           int rowsUpdated= query.executeUpdate();
           if(rowsUpdated<0){
               return false;
           }
        }



return true;

    }
}
