package pm.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// 项目数据访问：列表/查询/增删改/状态/成员/统计
// 表结构见 sql/pm.sql，全部使用 pm_ 前缀
public class ProjectDao {

    private static final String BASE_SELECT =
            "select p.id, p.project_no, p.name, p.project_type, p.priority, p.status,"
                    + " p.start_date, p.end_date, p.owner_id, p.budget, p.dept_name, p.description,"
                    + " p.create_time, p.creator, ifnull(u.user_name,'未指定') as owner_name,"
                    + " (select count(*) from pm_task t where t.project_id=p.id and t.del_flag=0) as task_count,"
                    + " (select count(*) from pm_task t where t.project_id=p.id and t.del_flag=0 and t.status='DONE') as done_count,"
                    + " (select ifnull(round(avg(t.progress)),0) from pm_task t where t.project_id=p.id and t.del_flag=0) as progress"
                    + " from pm_project p left join pm_user u on u.id=p.owner_id";

    // 数据权限：管理员看全部，其余看“自己负责 + 自己参与”
    public static String scope(String uid, boolean all) {
        if (all) {
            return "";
        }
        String u = Db.esc(uid);
        return " and (p.owner_id=" + u + " or exists(select 1 from pm_project_member m"
                + " where m.project_id=p.id and m.user_id=" + u + "))";
    }

    public static String buildWhere(String keyword, String status, String type, String priority,
                                    String uid, boolean all) {
        List<String> w = new ArrayList<String>();
        w.add("p.del_flag = 0");
        if (notBlank(keyword)) {
            String k = Db.esc(keyword);
            w.add("(p.name like '%" + k + "%' or p.project_no like '%" + k + "%')");
        }
        if (notBlank(status) && status.matches("[A-Z]{3,10}")) {
            w.add("p.status = '" + status + "'");
        }
        if (notBlank(type) && type.matches("[A-Z]{3,10}")) {
            w.add("p.project_type = '" + type + "'");
        }
        if (notBlank(priority) && priority.matches("[A-Z]{3,6}")) {
            w.add("p.priority = '" + priority + "'");
        }
        return " where " + String.join(" and ", w) + scope(uid, all);
    }

    private static String orderBy(String sort) {
        if ("end".equals(sort)) {
            return " order by p.end_date is null, p.end_date asc, p.id desc";
        }
        if ("budget".equals(sort)) {
            return " order by p.budget desc, p.id desc";
        }
        if ("name".equals(sort)) {
            return " order by p.name asc";
        }
        if ("new".equals(sort)) {
            return " order by p.id desc";
        }
        // 默认：优先级 + 截止时间（教学要求：排序不能只靠表头点击）
        return " order by field(p.priority,'HIGH','MID','LOW'), p.end_date is null, p.end_date asc, p.id desc";
    }

    public static List<Map<String, String>> list(String keyword, String status, String type,
                                                 String priority, String sort,
                                                 int page, int size, String uid, boolean all) {
        int offset = Math.max(0, (page - 1) * size);
        String sql = BASE_SELECT + buildWhere(keyword, status, type, priority, uid, all)
                + orderBy(sort) + " limit " + offset + "," + size;
        return Db.query(sql);
    }

    public static long count(String keyword, String status, String type, String priority,
                             String uid, boolean all) {
        return Db.queryLong("select count(*) from pm_project p"
                + buildWhere(keyword, status, type, priority, uid, all));
    }

    public static Map<String, String> get(String id) {
        if (!Db.isId(id)) {
            return null;
        }
        return Db.queryOne(BASE_SELECT + " where p.id=" + id + " and p.del_flag=0");
    }

    // 新增或修改；返回项目 id
    public static String save(String id, Map<String, String> p) {
        String name = Db.esc(p.get("name"));
        String type = norm(p.get("projectType"), "RND", "RND", "BUILD", "SERVICE", "OTHER");
        String priority = norm(p.get("priority"), "MID", "HIGH", "MID", "LOW");
        String status = norm(p.get("status"), "PENDING", "PENDING", "RUNNING", "PAUSED", "DONE", "CANCELED", "ARCHIVED");
        String start = Db.esc(p.get("startDate"));
        String end = Db.esc(p.get("endDate"));
        String ownerId = Db.isId(p.get("ownerId")) ? p.get("ownerId") : "NULL";
        String budget = p.get("budget") == null || p.get("budget").trim().isEmpty() ? "0" : Db.esc(p.get("budget"));
        if (!budget.matches("\\d{1,10}(\\.\\d{1,2})?")) {
            budget = "0";
        }
        String dept = Db.esc(p.get("deptName"));
        String desc = Db.esc(p.get("description"));

        if (Db.isId(id)) {
            StringBuilder sql = new StringBuilder("update pm_project set");
            sql.append(" name='").append(name).append("'");
            sql.append(", project_type='").append(type).append("'");
            sql.append(", priority='").append(priority).append("'");
            sql.append(", status='").append(status).append("'");
            sql.append(", start_date=").append(dateOrNull(start));
            sql.append(", end_date=").append(dateOrNull(end));
            sql.append(", owner_id=").append(ownerId);
            sql.append(", budget=").append(budget);
            sql.append(", dept_name='").append(dept).append("'");
            sql.append(", description='").append(desc).append("'");
            sql.append(" where id=").append(id);
            Db.update(sql.toString());
            return id;
        }

        String no = nextNo();
        Db.update("insert into pm_project(project_no,name,project_type,priority,status,start_date,end_date,"
                + "owner_id,budget,dept_name,description,create_time,creator,del_flag) values('"
                + no + "','" + name + "','" + type + "','" + priority + "','" + status + "',"
                + dateOrNull(start) + "," + dateOrNull(end) + "," + ownerId + "," + budget + ",'"
                + dept + "','" + desc + "','" + Db.now() + "','" + Db.esc(p.get("creator")) + "',0)");
        Map<String, String> row = Db.queryOne("select id from pm_project where project_no='" + no + "'");
        return row == null ? "" : row.get("id");
    }

    public static void changeStatus(String id, String status) {
        if (Db.isId(id) && status.matches("[A-Z]{3,10}")) {
            Db.update("update pm_project set status='" + status + "' where id=" + id + " and del_flag=0");
        }
    }

    public static void archive(String id) {
        changeStatus(id, "ARCHIVED");
    }

    // 逻辑删除
    public static void delete(String id) {
        if (Db.isId(id)) {
            Db.update("update pm_project set del_flag=1 where id=" + id);
        }
    }

    // 编号：PM-年份-4 位流水，冲突时顺延
    public static String nextNo() {
        String prefix = "PM-" + Db.year() + "-";
        long n = Db.queryLong("select count(*) from pm_project where project_no like '" + prefix + "%'");
        for (int i = 0; i < 1000; i++) {
            String no = prefix + String.format("%04d", n + 1 + i);
            if (Db.queryLong("select count(*) from pm_project where project_no='" + no + "'") == 0) {
                return no;
            }
        }
        return prefix + System.currentTimeMillis();
    }

    // 成员
    public static List<Map<String, String>> members(String projectId) {
        if (!Db.isId(projectId)) {
            return new ArrayList<Map<String, String>>();
        }
        return Db.query("select m.id, m.user_id, m.role_code, m.join_date, u.user_name, u.dept_name,"
                + " u.login_id from pm_project_member m join pm_user u on u.id=m.user_id"
                + " where m.project_id=" + projectId + " order by field(m.role_code,'MANAGER','MEMBER','VIEWER'), m.id");
    }

    public static String addMember(String projectId, String userId, String role) {
        if (!Db.isId(projectId) || !Db.isId(userId)) {
            return "成员参数不合法";
        }
        String r = norm(role, "MEMBER", "MANAGER", "MEMBER", "VIEWER");
        long exists = Db.queryLong("select count(*) from pm_project_member where project_id="
                + projectId + " and user_id=" + userId);
        if (exists > 0) {
            return "该成员已在项目中";
        }
        Db.update("insert into pm_project_member(project_id,user_id,role_code,join_date) values("
                + projectId + "," + userId + ",'" + r + "',curdate())");
        return null;
    }

    public static void removeMember(String projectId, String userId) {
        if (Db.isId(projectId) && Db.isId(userId)) {
            Db.update("delete from pm_project_member where project_id=" + projectId
                    + " and user_id=" + userId);
        }
    }

    public static boolean isMember(String projectId, String userId) {
        if (!Db.isId(projectId) || !Db.isId(userId)) {
            return false;
        }
        return Db.queryLong("select count(*) from pm_project_member where project_id=" + projectId
                + " and user_id=" + userId) > 0;
    }

    // 管理员、项目经理（负责人）或项目内 MANAGER 可管理项目
    public static boolean canManage(String projectId, String uid, String role, boolean admin) {
        if (admin) {
            return true;
        }
        if (!"PM".equals(role)) {
            return false;
        }
        Map<String, String> p = get(projectId);
        if (p != null && uid.equals(p.get("owner_id"))) {
            return true;
        }
        return Db.queryLong("select count(*) from pm_project_member where project_id=" + projectId
                + " and user_id=" + uid + " and role_code='MANAGER'") > 0;
    }

    public static boolean canView(String projectId, String uid, boolean admin) {
        if (admin) {
            return true;
        }
        Map<String, String> p = get(projectId);
        if (p == null) {
            return false;
        }
        return uid.equals(p.get("owner_id")) || isMember(projectId, uid);
    }

    // 下拉框：当前用户可见的项目
    public static List<Map<String, String>> forSelect(String uid, boolean all) {
        return Db.query("select p.id, p.project_no, p.name from pm_project p"
                + " where p.del_flag=0" + scope(uid, all) + " order by p.id desc");
    }

    public static List<Map<String, String>> overdue() {
        return Db.query(BASE_SELECT + " where p.del_flag=0 and p.end_date is not null"
                + " and p.end_date < curdate() and p.status in ('RUNNING','PAUSED','DONE')"
                + " order by p.end_date");
    }

    // 统计
    public static long countAll(String uid, boolean all) {
        return Db.queryLong("select count(*) from pm_project p where p.del_flag=0" + scope(uid, all));
    }

    public static List<Map<String, String>> countByStatus(String uid, boolean all) {
        return Db.query("select p.status, count(*) as cnt from pm_project p where p.del_flag=0"
                + scope(uid, all) + " group by p.status order by cnt desc");
    }

    public static Map<String, String> sumBudget(String uid, boolean all) {
        return Db.queryOne("select ifnull(sum(p.budget),0) as total from pm_project p"
                + " where p.del_flag=0" + scope(uid, all));
    }

    // 工具
    private static boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty() && !"undefined".equals(s) && !"null".equals(s);
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
