package pm.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DocDao {
//项目文档相关查询：项目文档表、项目名称与状态、上传文档的用户名称
// select
//    pm_doc.id,
//    pm_doc.project_id,
//    pm_doc.task_id,
//    pm_doc.name,
//    pm_doc.ext,
//    pm_doc.size,
//    pm_doc.path,
//    pm_doc.version,
//    pm_doc.visibility,
//    pm_doc.uploader_id,
//    pm_doc.create_time,
//    pm_doc.description,
//    pm_doc.tags,
//    pm_doc.category,
//
//    pm_project.name as project_name,
//    pm_project.status as project_status,
//
//    pm_user.user_name as uploader
//
//from pm_doc
//join pm_project on pm_project.id = pm_doc.project_id
//left join pm_user on pm_user.id = pm_doc.uploader_id;可能上传该文档的用户被删除但是文档仍然有用，比如员工离职
    private static final String BASE_SELECT =
            "select d.id, d.project_id, d.task_id, d.name, d.ext, d.size, d.path, d.version,"
                    + " d.visibility, d.uploader_id, d.create_time, d.description, d.tags, d.category,"
                    + " p.name as project_name, p.status as project_status, u.user_name as uploader"
                    + " from pm_doc d join pm_project p on p.id=d.project_id"
                    + " left join pm_user u on u.id=d.uploader_id";

    public static List<Map<String, String>> list(String projectId, String taskId, String status, String keyword, String orderBy,
                                                 String isRecycle, String uid, boolean all) {
        List<String> w = new ArrayList<String>();//判断应该查询什么内容
        if ("1".equals(isRecycle)) w.add("d.del_flag=1 and p.del_flag=0");//回收站
        else w.add("d.del_flag=0 and p.del_flag=0");//或正常列表

        if (Db.isId(projectId)){          //如果传入的参数projectId确实是一个已知项目
            w.add("d.project_id=" + projectId);    //就只查这个项目————否则查所有项目的文档
        if (Db.isId(taskId)) w.add("d.task_id=" + taskId);}   //任务依托于项目
        if (status != null && !status.trim().isEmpty()) w.add("p.status='" + Db.esc(status) + "'");
//有输入且输入不是空值 调用.esc自动转义防SQL注入
        if (keyword != null && !keyword.trim().isEmpty()) {
            String k = Db.esc(keyword);
            w.add("(d.name like '%" + k + "%' or d.description like '%" + k + "%' or d.tags like '%" + k + "%')");
        }//模糊搜索，项目文档搜索输入框

        String order = " order by d.create_time desc";//默认按上传时间大小倒叙排列，即新的在上边
        if ("name_asc".equals(orderBy)) order = " order by d.name asc";//选择按名称正序排列
        else if ("time_asc".equals(orderBy)) order = " order by d.create_time asc";//时间正序

        String where = " where "; //把之前筛选出来的存在w里的sql语句片段合并
        for (int i = 0; i < w.size(); i++) {
            if (i > 0) {
                where += " and ";
            }
            where += w.get(i);
        }
        where += ProjectDao.scope(uid, all);//调用来自ProjectDao的权限判断函数scope
        return Db.query(BASE_SELECT + where + order + " limit 200");
    }

    public static List<Map<String, String>> listTreeProjects(String uid, boolean all) {
        String sql = "select p.id, p.name, p.status, "//同时提取出状态方便写状态文件夹
                + " (select ifnull(sum(size),0) from pm_doc d where d.project_id = p.id and d.del_flag=0) as total_size, "//文件夹总大小，不计回收站
                + " (select ifnull(max(create_time),'') from pm_doc d where d.project_id = p.id and d.del_flag=0) as last_update "//文件夹最后更新时间
                + " from pm_project p where p.del_flag=0" + ProjectDao.scope(uid, all) + " order by p.id desc";
        return Db.query(sql);
    }

    public static List<Map<String, String>> listTreeTasks(String projectId) {
        String sql = "select t.id, t.title, "//任务文件夹
                + " (select ifnull(sum(size),0) from pm_doc d where d.task_id = t.id and d.del_flag=0) as total_size, "
                + " (select ifnull(max(create_time),'') from pm_doc d where d.task_id = t.id and d.del_flag=0) as last_update "
                + " from pm_task t where t.del_flag=0 and t.project_id=" + projectId + " order by t.id desc";
        return Db.query(sql);
    }

    public static List<Map<String, String>> listProjectMembers(String projectId, String selfUid) {//哪些人有权限可以看到文档
        String sql = "select u.id, u.user_name from pm_user u join pm_project_member m on u.id = m.user_id where m.project_id = " + projectId;
        if (Db.isId(selfUid)) sql += " and u.id != " + selfUid;//上传者自己默认可以看到文档
        return Db.query(sql);
    }

    public static List<Map<String, String>> listTaskMembers(String projectId, String taskId, String selfUid) {
        String sql = "select id, user_name from pm_user where id in ("
                + " select assignee_id from pm_task where id=" + taskId
                + " union select owner_id from pm_project where id=" + projectId + ")";
        if (Db.isId(selfUid)) sql += " and id != " + selfUid;
        return Db.query(sql);
    }

    public static Map<String, String> get(String id) {   //直接通过ID找文档的抓手
        if (!Db.isId(id)) return null;
        return Db.queryOne(BASE_SELECT + " where d.id=" + id);
    }

    public static int nextVersion(String projectId, String name) {//未完成！版本号的更新！
        int v = 1;   //问题1：是否管回收站里的文档的版本号 问题2：改名改成同名怎么处理
        return v;    //问题3：不同任务同名文档怎么处理
    }

    public static void add(String projectId, String taskId, String name, String ext, long size,
                           String path, String visibility, String uploaderId, String description, String tags) {
        int version = nextVersion(projectId, name);//暂时使用这个获取新存入文档应得的版本号
        if (path != null) path = path.replace('\\', '/');// 替换防止数据库内存的路径乱码
        String docTaskId = "0"; //未关联则设置关联任务ID为0方便辨认
        if (Db.isId(taskId)) {  //文档是否关联着一个任务？
            docTaskId = taskId;
        }
        Db.update("insert into pm_doc(project_id,task_id,name,ext,size,path,version,visibility,"
                + "uploader_id,create_time,del_flag,description,tags) values(" + Db.esc(projectId) + ","
                + docTaskId + ",'" + Db.esc(name) + "','" + Db.esc(ext) + "',"
                + size + ",'" + Db.esc(path) + "'," + version + ",'" + Db.esc(visibility) + "',"
                + Db.esc(uploaderId) + ",'" + Db.now() + "',0,'" + Db.esc(description) + "','" + Db.esc(tags) + "')");//now获取当前时间
    }

    public static void updateInfo(String id, String name, String description, String tags) {
        if (Db.isId(id)) {
            Db.update("update pm_doc set name='" + Db.esc(name) + "', description='" + Db.esc(description)
                    + "', tags='" + Db.esc(tags) + "' where id=" + id); //不修改文件内容，只修改名字、描述和标签
        }  //暂定不修改create_time
    }

    public static void delete(String id) {  //进入回收站
        if (Db.isId(id)) Db.update("update pm_doc set del_flag=1 where id=" + id);
    }

    public static void forceDelete(String id) {  //彻底删除(暂时只删除数据库记录)
        if (Db.isId(id)) {  //暂时未删除真实文件，可能回滚项目功能会使用？待回滚开发完成
            Db.update("delete from pm_doc where id=" + id);
            Db.update("delete from pm_doc_permission where doc_id=" + id);
        }
    }

    public static void restore(String id) {
        if (Db.isId(id)) Db.update("update pm_doc set del_flag=0 where id=" + id);
    }

    public static void addPermissions(String docId, String[] userIds) {  //只允许指定人员查阅
        if (userIds == null) return;  //数组为空，未传入“指定人员”，不执行
        for (int i = 0; i < userIds.length; i++) {
            String uid = userIds[i];
            if (Db.isId(uid)) {  //传入的“指定人员"id合法才写入表
                Db.update("insert ignore into pm_doc_permission(doc_id,user_id) values(" + docId + "," + uid + ")");
            }
        }
    }
    public static boolean hasPermission(String docId, String userId) {  //哪些是”指定人员“
        return Db.queryLong("select count(*) from pm_doc_permission where doc_id=" + docId + " and user_id=" + userId) > 0;
    }
}