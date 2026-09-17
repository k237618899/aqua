package icu.samnyan.aqua.api.model.resp.sega.diva;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Validation result for DIVA song batch upsert dry-run.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DivaSongBatchValidateResponse {

    private boolean valid;

    private int total;

    private int validCount;

    private int errorCount;

    private List<String> errors = new ArrayList<>();
}
