package com.tse.core_application.repository;

import java.util.HashMap;

public class TaskHistoryCustomInterfaceImpl implements TaskHistoryCustomInterface{
//    @PersistenceContext
//    private EntityManagxer entityManager;
    @Override
    public final void fetchTaskHistoryForTasksWithFieldChange(Long taskId, String field, HashMap<String,Object> filters,String orderBy) {
//       List<TaskHistory> result=new ArrayList<TaskHistory>();
//       String whereStr = "th.taskId=:taskId";
//       Iterator filterIterator = filters.entrySet().iterator();
//
//       if(filterIterator.hasNext()) {
//            Map.Entry filterElement = (Map.Entry) filterIterator.next();
//            whereStr+=" and th."+filterElement.getKey()+"=:"+filterElement.getKey();
//       }
//       String queryStr = "Select th from TaskHistory th where "+whereStr+
//                "taskHistoryId in (select min(th1.taskHistoryId) from TaskHistory th1 group by :distinctField)";
//
//       Query query = entityManager.createQuery("Select th from TaskHistory th where th.taskId = :taskId and " +
//                "taskHistoryId in (select min(th1.taskHistoryId) from TaskHistory th1 group by :distinctField)")
//                .setParameter("taskId",taskId)
//                .setParameter("distinctField",field);
//       for (Map.Entry mapElement : filters.entrySet()) {
//            String key = (String)mapElement.getKey();
//            query=query.setParameter(key,mapElement.getValue());
//       }
//       List tasksHistoryResultList = query.getResultList();
//        for(Object taskHistory:tasksHistoryResultList){
//            if(taskHistory instanceof TaskHistory ){
//                result.add((TaskHistory) taskHistory);
//            }
//        }
//        return result;
    }
}
