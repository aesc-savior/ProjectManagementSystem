package pm.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// 任务：创建/WBS/状态与进度/查询/统计
public class TaskDao {

    private static final String BASE_SELECT =
            "select t.id, t.project_id, t.parent_id, t.title, t.description, t.assignee_id,"
                    + " t.priority, t.status, t.progress, t.start_date, t.due_date,"
                    + " t.estimate_hours, t.actual_hours, t.create_time, t.creator,"
                    + " p.project_no, p.name as project_name, ifnull(u.user_name,'未分配') as assignee,"
                    + " ifnull(pt.title,'') as parent_title"
                    + " from pm_task t join pm_project p on p.id=t.project_id"
                    + " left join pm_user u on u.id=t.assignee_id"
                    + " left join pm_task pt on pt.id=t.parent_id";

    public static String buildWhere(String keyword, String projectId, String assigneeId,
                                    String status, String uid, boolean all) {
        List<String> w = new ArrayList<String>();
        w.add("t.del_flag=0 and p.del_flag=0");
        if (notBlank(keyword)) {
            String k = Db.esc(keyword);
            w.add("(t.title like '%" + k + "%' or t.description like '%" + k + "%')");
        }
        if (Db.isId(projectId)) {
            w.add("t.project_id=" + projectId);
        }
        if (Db.isId(assigneeId)) {
            w.add("t.assignee_id=" + assigneeId);
        }
        if (notBlank(status) && status.matches("[A-Z]{3,10}")) {
            w.add("t.status='" + status + "'");
        }
        return " where " + String.join(" and ", w) + ProjectDao.scope(uid, all);
    }

    private static String orderBy(String sort) {
        if ("due".equals(sort)) {
            return " order by t.due_date is null, t.due_date asc, t.id desc";
        }
        if ("progress".equals(sort)) {
            return " order by t.progress desc, t.id desc";
        }
        return " order by t.project_id desc, t.parent_id, t.id";
    }

    public static List<Map<String, String>> list(String keyword, String projectId, String assigneeId,
                                                 String status, String sort, int page, int size,
                                                 String uid, boolean all) {
        int offset = Math.max(0, (page - 1) * size);
        return Db.query(BASE_SELECT + buildWhere(keyword, projectId, assigneeId, status, uid, all)
                + orderBy(sort) + " limit " + offset + "," + size);
    }

    public static long count(String keyword, String projectId, String assigneeId, String status,
                             String uid, boolean all) {
        return Db.queryLong("select count(*) from pm_task t join pm_project p on p.id=t.project_id"
                + buildWhere(keyword, projectId, assigneeId, status, uid, all));
    }

    public static Map<String, String> get(String id) {
        if (!Db.isId(id)) {
            return null;
        }
        return Db.queryOne(BASE_SELECT + " where t.id=" + id + " and t.del_flag=0");
    }

    // 新增或修改；返回任务 id
    public static String save(String id, Map<String, String> p) {
        String projectId = Db.isId(p.get("projectId")) ? p.get("projectId") : "0";
        String parentId = Db.isId(p.get("parentId")) ? p.get("parentId") : "0";
        String title = Db.esc(p.get("title"));
        String desc = Db.esc(p.get("description"));
        String assignee = Db.isId(p.get("assigneeId")) ? p.get("assigneeId") : "NULL";
        String priority = norm(p.get("priority"), "MID", "HIGH", "MID", "LOW");
        String status = norm(p.get("status"), "TODO", "TODO", "DOING", "PAUSED", "DONE", "CANCELED");
        int progress = Math.max(0, Math.min(100, parseInt(p.get("progress"), 0)));
        if ("DONE".equals(status)) {
            progress = 100;
        } else if (progress == 100) {
            status = "DONE";
        } else if (progress > 0 && "TODO".equals(status)) {
            status = "DOING";
        }
        String start = Db.esc(p.get("startDate"));
        String due = Db.esc(p.get("dueDate"));
        String estimate = p.get("estimateHours") == null || p.get("estimateHours").trim().isEmpty() ? "0" : Db.esc(p.get("estimateHours"));
        String actual = p.get("actualHours") == null || p.get("actualHours").trim().isEmpty() ? "0" : Db.esc(p.get("actualHours"));
        if (!estimate.matches("\\d{1,8}(\\.\\d{1,2})?")) estimate = "0";
        if (!actual.matches("\\d{1,8}(\\.\\d{1,2})?")) actual = "0";

        if (Db.isId(id)) {
            StringBuilder sql = new StringBuilder("update pm_task set");
            sql.append(" project_id=").append(projectId);
            sql.append(", parent_id=").append(parentId);
            sql.append(", title='").append(title).append("'");
            sql.append(", description='").append(desc).append("'");
            sql.append(", assignee_id=").append(assignee);
            sql.append(", priority='").append(priority).append("'");
            sql.append(", status='").append(status).append("'");
            sql.append(", progress=").append(progress);
            sql.append(", start_date=").append(dateOrNull(start));
            sql.append(", due_date=").append(dateOrNull(due));
            sql.append(", estimate_hours=").append(estimate);
            sql.append(", actual_hours=").append(actual);
            sql.append(" where id=").append(id);
            Db.update(sql.toString());
            rollup(parentId);
            return id;
        }

        Db.update("insert into pm_task(project_id,parent_id,title,description,assignee_id,priority,status,"
                + "progress,start_date,due_date,estimate_hours,actual_hours,create_time,creator,del_flag) values("
                + projectId + "," + parentId + ",'" + title + "','" + desc + "'," + assignee + ",'"
                + priority + "','" + status + "'," + progress + "," + dateOrNull(start) + ","
                + dateOrNull(due) + "," + estimate + "," + actual + ",'" + Db.now() + "','"
                + Db.esc(p.get("creator")) + "',0)");
        Map<String, String> row = Db.queryOne("select id from pm_task where project_id=" + projectId
                + " and title='" + title + "' order by id desc limit 1");
        String newId = row == null ? "" : row.get("id");
        rollup(parentId);
        return newId;
    }

    // 只看进度：用于成员在任务页快速更新
    public static void updateProgress(String id, int progress, String actualHours) {
        if (!Db.isId(id)) {
            return;
        }
        progress = Math.max(0, Math.min(100, progress));
        String status = progress >= 100 ? "DONE" : (progress > 0 ? "DOING" : "TODO");
        String actual = actualHours != null && actualHours.matches("\\d{1,8}(\\.\\d{1,2})?") ? actualHours : "0";
        Db.update("update pm_task set progress=" + progress + ", status='" + status
                + "', actual_hours=" + actual + " where id=" + id);
        Map<String, String> t = Db.queryOne("select parent_id from pm_task where id=" + id);
        if (t != null) {
            rollup(t.get("parent_id"));
        }
    }

    // 父任务进度 = 子任务平均进度
    public static void rollup(String parentId) {
        if (!Db.isId(parentId) || "0".equals(parentId)) {
            return;
        }
        Map<String, String> agg = Db.queryOne("select count(*) as n, ifnull(round(avg(progress)),0) as p,"
                + " sum(case when status='DONE' then 1 else 0 end) as d from pm_task"
                + " where parent_id=" + parentId + " and del_flag=0");
        if (agg == null || "0".equals(agg.get("n"))) {
            return;
        }
        int p = parseInt(agg.get("p"), 0);
        String status = p >= 100 ? "DONE" : (p > 0 ? "DOING" : "TODO");
        Db.update("update pm_task set progress=" + p + ", status='" + status + "' where id=" + parentId);
        Map<String, String> t = Db.queryOne("select parent_id from pm_task where id=" + parentId);
        if (t != null) {
            rollup(t.get("parent_id"));
        }
    }

    public static String delete(String id) {
        if (!Db.isId(id)) {
            return "任务编号不合法";
        }
        long children = Db.queryLong("select count(*) from pm_task where parent_id=" + id + " and del_flag=0");
        if (children > 0) {
            return "该任务还有 " + children + " 个子任务，请先删除子任务";
        }
        Db.update("update pm_task set del_flag=1 where id=" + id);
        return null;
    }

    // 下拉：某项目的顶层任务（作为父任务备选）
    public static List<Map<String, String>> topTasks(String projectId) {
        if (!Db.isId(projectId)) {
            return new ArrayList<Map<String, String>>();
        }
        return Db.query("select id, title from pm_task where project_id=" + projectId
                + " and parent_id=0 and del_flag=0 order by id");
    }

    public static long countMine(String uid) {
        return Db.queryLong("select count(*) from pm_task where del_flag=0 and assignee_id=" + Db.esc(uid)
                + " and status in ('TODO','DOING')");
    }

    public static List<Map<String, String>> countByStatus(String uid, boolean all) {
        return Db.query("select t.status, count(*) as cnt from pm_task t"
                + " join pm_project p on p.id=t.project_id"
                + buildWhere("", "", "", "", uid, all) + " group by t.status order by cnt desc");
    }

    private static boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return def;
        }
    }

    private static String dateOrNull(String d) {
        return d != null && d.matches("\\d{4}-\\d{2}-\\d{2}") ? "'" + d + "'" : "null";
    }

    private static String norm(String v, String def, String... allowed) {
        if (v != null) {
            for (String a : allowed) {
                if (a.equals(v)) {
                    return v;
                }
            }
        }
        return def;
    }
}
