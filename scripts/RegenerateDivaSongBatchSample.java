import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class RegenerateDivaSongBatchSample {
    static String esc(String s){ if(s==null) return null; return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r").replace("\t","\\t"); }
    static String col(ResultSet rs, List<String> names) throws SQLException {
        ResultSetMetaData md=rs.getMetaData(); int c=md.getColumnCount();
        for(String n:names){ for(int i=1;i<=c;i++){ if(md.getColumnLabel(i).equalsIgnoreCase(n)) return rs.getString(i); } }
        return null;
    }
    static Integer coli(ResultSet rs, List<String> names) throws SQLException {
        String v=col(rs,names); if(v==null||v.isBlank()) return null;
        try { return Integer.parseInt(v.trim()); } catch(Exception e) { return null; }
    }
    static String tsOrDefault(String raw, LocalDateTime d){
        if(raw==null||raw.isBlank()) return d.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String r=raw.trim().replace(' ','T');
        if(r.length()==10) r=r+"T00:00:00";
        return r;
    }

    public static void main(String[] args) throws Exception {
        String dbPath = "db.sqlite";
        String outPath = "docs/dev/diva-song-batch-sample.json";
        LocalDateTime now = LocalDateTime.now().withNano(0);
        LocalDateTime demoEndDef = now.plusDays(20);
        LocalDateTime playableEndDef = LocalDateTime.of(2099,12,1,0,0,0);

        Class.forName("org.sqlite.JDBC");
        List<String> songsOut = new ArrayList<>();
        try(Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath)){
            conn.setReadOnly(true);
            String songSql = "SELECT p.* FROM diva_pv p WHERE EXISTS (SELECT 1 FROM diva_pv_entry e WHERE e.pv_id=p.pv_id) ORDER BY p.pv_id";
            try(Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(songSql)){
                while(rs.next() && songsOut.size()<5){
                    Integer pvId = coli(rs, Arrays.asList("pv_id","pvid","pvId","id"));
                    if(pvId==null) continue;
                    Integer bpm = coli(rs, Arrays.asList("bpm"));
                    String songName = col(rs, Arrays.asList("song_name","name","title","songName"));
                    String songNameEng = col(rs, Arrays.asList("song_name_eng","name_eng","songNameEng","title_eng"));
                    String songNameReading = col(rs, Arrays.asList("song_name_reading","name_reading","songNameReading"));
                    String arranger = col(rs, Arrays.asList("arranger"));
                    String lyrics = col(rs, Arrays.asList("lyrics"));
                    String music = col(rs, Arrays.asList("music","composer"));
                    Integer performer = coli(rs, Arrays.asList("performer_number","performer_num","performerNumber"));
                    if(performer==null) performer=1;

                    String entrySql = "SELECT * FROM diva_pv_entry WHERE pv_id=? ORDER BY rowid";
                    Map<Integer,String> byDifficulty = new LinkedHashMap<>();
                    try(PreparedStatement ps = conn.prepareStatement(entrySql)){
                        ps.setInt(1,pvId);
                        try(ResultSet er = ps.executeQuery()){
                            while(er.next()){
                                Integer diff = coli(er, Arrays.asList("difficulty","diff"));
                                if(diff==null || byDifficulty.containsKey(diff)) continue;
                                Integer version = coli(er, Arrays.asList("version")); if(version==null) version=1;
                                Integer edition = coli(er, Arrays.asList("edition")); if(edition==null) edition=0;
                                String demoStart = tsOrDefault(col(er, Arrays.asList("demo_start","demoStart")), now);
                                String demoEnd = tsOrDefault(col(er, Arrays.asList("demo_end","demoEnd")), demoEndDef);
                                String playableStart = tsOrDefault(col(er, Arrays.asList("playable_start","playableStart")), now);
                                String playableEnd = tsOrDefault(col(er, Arrays.asList("playable_end","playableEnd")), playableEndDef);

                                String entry = "      {\n"+
                                    "        \"difficulty\": "+diff+",\n"+
                                    "        \"version\": "+version+",\n"+
                                    "        \"edition\": "+edition+",\n"+
                                    "        \"demoStart\": \""+esc(demoStart)+"\",\n"+
                                    "        \"demoEnd\": \""+esc(demoEnd)+"\",\n"+
                                    "        \"playableStart\": \""+esc(playableStart)+"\",\n"+
                                    "        \"playableEnd\": \""+esc(playableEnd)+"\"\n"+
                                    "      }";
                                byDifficulty.put(diff, entry);
                            }
                        }
                    }
                    if(byDifficulty.isEmpty()) continue;

                    StringBuilder song = new StringBuilder();
                    song.append("  {\n");
                    song.append("    \"pvId\": ").append(pvId).append(",\n");
                    song.append("    \"bpm\": ").append(bpm==null?"null":bpm).append(",\n");
                    song.append("    \"songName\": \"").append(esc(songName==null?"":songName)).append("\",\n");
                    song.append("    \"songNameEng\": \"").append(esc(songNameEng==null?"":songNameEng)).append("\",\n");
                    song.append("    \"songNameReading\": \"").append(esc(songNameReading==null?"":songNameReading)).append("\",\n");
                    song.append("    \"arranger\": \"").append(esc(arranger==null?"":arranger)).append("\",\n");
                    song.append("    \"lyrics\": \"").append(esc(lyrics==null?"":lyrics)).append("\",\n");
                    song.append("    \"music\": \"").append(esc(music==null?"":music)).append("\",\n");
                    song.append("    \"performerNumber\": ").append(performer).append(",\n");
                    song.append("    \"entries\": [\n");
                    int i=0, n=byDifficulty.size();
                    for(String e : byDifficulty.values()){ song.append(e); if(++i<n) song.append(","); song.append("\n"); }
                    song.append("    ]\n");
                    song.append("  }");
                    songsOut.add(song.toString());
                }
            }
        }

        if(songsOut.size() < 5) throw new RuntimeException("Could only build "+songsOut.size()+" songs meeting constraints.");

        StringBuilder json = new StringBuilder();
        json.append("[\n");
        for(int i=0;i<5;i++){ json.append(songsOut.get(i)); if(i<4) json.append(","); json.append("\n"); }
        json.append("]\n");

        Files.write(Paths.get(outPath), json.toString().getBytes(StandardCharsets.UTF_8));
        System.out.println("Wrote " + outPath + " with 5 songs.");
    }
}