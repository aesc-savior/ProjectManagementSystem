package archive.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 档案数据访问：列表/查询/增删改/附件/分类/统计。
 * 表结构见 sql/archive.sql，全部使用 da_ 前缀。
 */
public class ArchiveDao {

    private static final String BASE_SELECT =
            "select a.id, a.archive_no, a.title, a.category_id, a.secret_level, a.keep_years,"
                    + " a.dept_name, a.owner_name, a.status, a.archive_time, a.remark,"
                    + " a.create_time, a.creator, ifnull(c.name,'未分类') as category_name"
                    + " from da_archive a left join da_category c on c.id = a.category_id";

    /** 组合条件：关键词（名称/编号）、分类、年度、密级、状态。 */
    public static String buildWhere(String keyword, String categoryId, String year,
                                    String secretLevel, String status) {
        List<String> w = new ArrayList<String>();
        w.add("a.del_flag = 0");
        if (notBlank(keyword)) {
            String k = Db.esc(keyword);
            w.add("(a.title like '%" + k + "%' or a.archive_no like '%" + k + "%')");
        }
        if (Db.isId(categoryId)) {
            w.add("a.category_id = " + categoryId);
        }
        if (notBlank(year) && year.matches("\\d{4}")) {
            w.add("year(ifnull(a.archive_time, a.create_time)) = " + year);
        }
        if (notBlank(secretLevel) && secretLevel.matches("[1-4]")) {
            w.add("a.secret_level = " + secretLevel);
        }
        if (notBlank(status) && status.matches("[A-Z]{3,10}")) {
            w.add("a.status = '" + status + "'");
        }
        return " where " + String.join(" and ", w);
    }

    public static List<Map<String, String>> list(String keyword, String categoryId, String year,
                                                 String secretLevel, String status,
                                                 int page, int size) {
        int offset = Math.max(0, (page - 1) * size);
        String sql = BASE_SELECT + buildWhere(keyword, categoryId, year, secretLevel, status)
                + " order by a.id desc limit " + offset + "," + size;
        return Db.query(sql);
    }

    public static long count(String keyword, String categoryId, String year,
                             String secretLevel, String status) {
        String sql = "select count(*) from da_archive a"
                + buildWhere(keyword, categoryId, year, secretLevel, status);
        return Db.queryLong(sql);
    }

    public static Map<String, String> get(String id) {
        if (!Db.isId(id)) {
            return null;
        }
        return Db.queryOne(BASE_SELECT + " where a.id = " + id);
    }

    /** 新增或修改；返回档案 id。 */
    public static String save(String id, Map<String, String> p) {
        String title = Db.esc(p.get("title"));
        String categoryId = Db.isId(p.get("categoryId")) ? p.get("categoryId") : "0";
        String secret = normSecret(p.get("secretLevel"));
        String keep = normKeep(p.get("keepYears"));
        String dept = Db.esc(p.get("deptName"));
        String owner = Db.esc(p.get("ownerName"));
        String remark = Db.esc(p.get("remark"));
        String status = normStatus(p.get("status"));
        String archiveTime = Db.esc(p.get("archiveTime"));

        if (Db.isId(id)) {
            StringBuilder sql = new StringBuilder("update da_archive set");
            sql.append(" title='").append(title).append("'");
            sql.append(", category_id=").append(categoryId);
            sql.append(", secret_level=").append(secret);
            sql.append(", keep_years='").append(keep).append("'");
            sql.append(", dept_name='").append(dept).append("'");
            sql.append(", owner_name='").append(owner).append("'");
            sql.append(", status='").append(status).append("'");
            sql.append(", remark='").append(remark).append("'");
            if (notBlank(archiveTime)) {
                sql.append(", archive_time='").append(archiveTime).append("'");
            }
            sql.append(" where id=").append(id);
            Db.update(sql.toString());
            return id;
        }

        String no = nextArchiveNo();
        String time = notBlank(archiveTime) ? archiveTime : ("ARCHIVED".equals(status) ? Db.now() : null);
        StringBuilder sql = new StringBuilder("insert into da_archive"
                + "(archive_no,title,category_id,secret_level,keep_years,dept_name,owner_name,"
                + "status,archive_time,remark,create_time,creator,del_flag) values(");
        sql.append("'").append(no).append("'");
        sql.append(",'").append(title).append("'");
        sql.append(",").append(categoryId);
        sql.append(",").append(secret);
        sql.append(",'").append(keep).append("'");
        sql.append(",'").append(dept).append("'");
        sql.append(",'").append(owner).append("'");
        sql.append(",'").append(status).append("'");
        sql.append(",").append(time == null ? "null" : "'" + time + "'");
        sql.append(",'").append(remark).append("'");
        sql.append(",'").append(Db.now()).append("'");
        sql.append(",'").append(Db.esc(p.get("creator"))).append("'");
        sql.append(",0)");
        Db.update(sql.toString());
        Map<String, String> row = Db.queryOne("select id from da_archive where archive_no='" + no + "'");
        return row == null ? "" : row.get("id");
    }

    /** 逻辑删除。 */
    public static void delete(String id) {
        if (Db.isId(id)) {
            Db.update("update da_archive set del_flag=1 where id=" + id);
        }
    }

    /** 归档：状态改为 ARCHIVED 并记录归档时间。 */
    public static void archive(String id) {
        if (Db.isId(id)) {
            Db.update("update da_archive set status='ARCHIVED', archive_time='" + Db.now()
                    + "' where id=" + id + " and del_flag=0");
        }
    }

    /** 编号：DA-年份-4 位流水，冲突时顺延。 */
    public static String nextArchiveNo() {
        String prefix = "DA-" + Db.year() + "-";
        long n = Db.queryLong("select count(*) from da_archive where archive_no like '" + prefix + "%'");
        for (int i = 0; i < 1000; i++) {
            String no = prefix + String.format("%04d", n + 1 + i);
            if (Db.queryLong("select count(*) from da_archive where archive_no='" + no + "'") == 0) {
                return no;
            }
        }
        return prefix + System.currentTimeMillis();
    }

    // ---------------- 分类 ----------------
    public static List<Map<String, String>> categories() {
        return Db.query("select id, parent_id, name, level from da_category where del_flag=0"
                + " order by parent_id, sort, id");
    }

    public static void addCategory(String name, String parentId) {
        if (!notBlank(name)) {
            return;
        }
        String pid = Db.isId(parentId) ? parentId : "0";
        int level = "0".equals(pid) ? 1 : 2;
        Db.update("insert into da_category(parent_id,name,level,sort,del_flag) values("
                + pid + ",'" + Db.esc(name) + "'," + level + ",0,0)");
    }

    /** 删除分类前校验：有档案或子分类时拒绝，返回提示语；成功返回 null。 */
    public static String deleteCategory(String id) {
        if (!Db.isId(id)) {
            return "分类编号不合法";
        }
        long used = Db.queryLong("select count(*) from da_archive where category_id=" + id + " and del_flag=0");
        if (used > 0) {
            return "该分类下还有 " + used + " 条档案，不能删除";
        }
        long children = Db.queryLong("select count(*) from da_category where parent_id=" + id + " and del_flag=0");
        if (children > 0) {
            return "该分类还有子分类，请先删除子分类";
        }
        Db.update("update da_category set del_flag=1 where id=" + id);
        return null;
    }

    // ---------------- 附件 ----------------
    public static List<Map<String, String>> files(String archiveId) {
        if (!Db.isId(archiveId)) {
            return new ArrayList<Map<String, String>>();
        }
        return Db.query("select id, archive_id, file_name, store_name, file_path, file_size, file_type,"
                + " create_time from da_archive_file where del_flag=0 and archive_id=" + archiveId
                + " order by id desc");
    }

    public static void addFile(String archiveId, String fileName, String storeName,
                               String filePath, long size, String type) {
        Db.update("insert into da_archive_file(archive_id,file_name,store_name,file_path,file_size,"
                + "file_type,create_time,del_flag) values(" + archiveId + ",'" + Db.esc(fileName) + "','"
                + Db.esc(storeName) + "','" + Db.esc(filePath) + "'," + size + ",'"
                + Db.esc(type) + "','" + Db.now() + "',0)");
    }

    public static Map<String, String> file(String fileId) {
        if (!Db.isId(fileId)) {
            return null;
        }
        return Db.queryOne("select * from da_archive_file where id=" + fileId + " and del_flag=0");
    }

    public static void deleteFile(String fileId) {
        if (Db.isId(fileId)) {
            Db.update("update da_archive_file set del_flag=1 where id=" + fileId);
        }
    }

    // ---------------- 统计 ----------------
    public static Map<String, String> stats() {
        Map<String, String> s = new java.util.LinkedHashMap<String, String>();
        s.put("total", String.valueOf(Db.queryLong("select count(*) from da_archive where del_flag=0")));
        s.put("archived", String.valueOf(Db.queryLong(
                "select count(*) from da_archive where del_flag=0 and status='ARCHIVED'")));
        s.put("borrowed", String.valueOf(Db.queryLong(
                "select count(*) from da_archive where del_flag=0 and status='BORROWED'")));
        s.put("pending", String.valueOf(Db.queryLong(
                "select count(*) from da_archive where del_flag=0 and status='PENDING'")));
        s.put("borrowPending", String.valueOf(Db.queryLong(
                "select count(*) from da_borrow where status='PENDING'")));
        s.put("users", String.valueOf(Db.queryLong("select count(*) from da_user where del_flag=0")));
        s.put("categories", String.valueOf(Db.queryLong(
                "select count(*) from da_category where del_flag=0")));
        return s;
    }

    // ---------------- 工具 ----------------
    private static boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty() && !"undefined".equals(s) && !"null".equals(s);
    }

    private static String normSecret(String v) {
        return v != null && v.matches("[1-4]") ? v : "1";
    }

    private static String normKeep(String v) {
        if (v == null) {
            return "LONG";
        }
        if ("SHORT".equals(v) || "LONG".equals(v) || "FOREVER".equals(v)) {
            return v;
        }
        return "LONG";
    }

    private static String normStatus(String v) {
        if (v == null) {
            return "PENDING";
        }
        if ("PENDING".equals(v) || "ARCHIVED".equals(v) || "DESTROYED".equals(v)) {
            return v;
        }
        return "PENDING";
    }
}
