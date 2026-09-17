package icu.samnyan.aqua.sega.diva.handler.databank;

import icu.samnyan.aqua.sega.diva.handler.BaseHandler;
import icu.samnyan.aqua.sega.diva.model.request.BaseRequest;
import icu.samnyan.aqua.sega.diva.model.response.databank.PvListResponse;
import icu.samnyan.aqua.sega.diva.util.DivaMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * @author samnyan (privateamusement@protonmail.com)
 */
@Component
public class PvListHandler extends BaseHandler {

    private static final Logger logger = LoggerFactory.getLogger(PvListHandler.class);

    private final PvListCacheService pvListCacheService;

    public PvListHandler(DivaMapper mapper, PvListCacheService pvListCacheService) {
        super(mapper);
        this.pvListCacheService = pvListCacheService;
    }

    public String handle(BaseRequest request) {
        long start = System.currentTimeMillis();

        // A2: the expensive content (4 difficulty queries + URI encoding) is built
        // once and cached; the per-request fields (req_id, timestamp) are added here
        // so they stay fresh for every cabinet.
        PvListContent content = pvListCacheService.getPvListContent();

        PvListResponse response = new PvListResponse(
                request.getCmd(),
                request.getReq_id(),
                "ok",
                LocalDateTime.now(),
                content.getContent());

        String resp = this.build(mapper.toMap(response));

        long cost = System.currentTimeMillis() - start;

        // A1: never log the full response body (it can be huge at 900+ songs and
        // burns disk I/O + GC). Log a compact, searchable summary instead.
        logger.info("pv_list response: reqId={}, length={}, counts(easy/normal/hard/extreme)={}/{}/{}/{}, cost={}ms",
                request.getReq_id(), resp.length(),
                content.getEasyCount(), content.getNormalCount(), content.getHardCount(), content.getExtremeCount(),
                cost);

        return resp;
    }
}
