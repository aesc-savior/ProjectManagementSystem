package pm.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// 项目文档：上传、下载、查询、删除
public class DocDao {

    private static final String BASE_SELECT =
            "select d.id, d.project_id, d.task_id, d.name, d.ext, d.size, d.path, d.version,"
                    + " d.visibility, d.uploader_id, d.create_time, p.name as project_name,"
                    + " ifnull(u.user_name,'') as uploader"
                    + " from pm_doc d join pm_project p on p.id=d.project_id"
                    + " left join pm_user u on u.id=d.uploader_id";

    public static List<Map<String, String>> list(String projectId, String keyword, String uid, boolean all) {
        List<String> w = new ArrayList<String>();
        w.add("d.del_flag=0 and p.del_flag=0");
        if (Db.isId(projectId)) {
            w.add("d.project_id=" + projectId);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            String k = Db.esc(keyword);
            w.add("d.name like '%" + k + "%'");
        }
        String where = " where " + String.join(" and ", w) + ProjectDao.scope(uid, all);
        return Db.query(BASE_SELECT + where + " order by d.id desc limit 200");
    }

    public static Map<String, String> get(String id) {
        if (!Db.isId(id)) {
            return null;
        }
        return Db.queryOne(BASE_SELECT + " where d.id=" + id + " and d.del_flag=0");
    }

    public static int nextVersion(String projectId, String name) {
        Map<String, String> row = Db.queryOne("select ifnull(max(version),0) as v from pm_doc"
                + " where project_id=" + Db.esc(projectId) + " and name='" + Db.esc(name) + "'");
        int v = 1;
        try {
            v = Integer.parseInt(row.get("v")) + 1;
        } catch (Exception ignore) {
        }
        return v;
    }

    public static void add(String projectId, String taskId, String name, String ext, long size,
                           String path, String visibility, String uploaderId) {
        int version = nextVersion(projectId, name);
        // 路径统一为正斜杠：SQL 为字符串拼接，Windows 反斜杠会被 MySQL 当作转义符吃掉，
        // 导致落库路径失真、下载 404。Java File 在 Windows 下同样支持正斜杠
        if (path != null) {
            path = path.replace('\\', '/');
        }
        Db.update("insert into pm_doc(project_id,task_id,name,ext,size,path,version,visibility,"
                + "uploader_id,create_time,del_flag) values(" + Db.esc(projectId) + ","
                + (Db.isId(taskId) ? taskId : "0") + ",'" + Db.esc(name) + "','" + Db.esc(ext) + "',"
                + size + ",'" + Db.esc(path) + "'," + version + ",'" + Db.esc(visibility) + "',"
                + Db.esc(uploaderId) + ",'" + Db.now() + "',0)");
    }

    public static void delete(String id) {
        if (Db.isId(id)) {
            Db.update("update pm_doc set del_flag=1 where id=" + id);
        }
    }

    public static long count(String uid, boolean all) {
        return Db.queryLong("select count(*) from pm_doc d join pm_project p on p.id=d.project_id"
                + " where d.del_flag=0 and p.del_flag=0" + ProjectDao.scope(uid, all));
    }
}
