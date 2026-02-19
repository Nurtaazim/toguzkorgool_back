package com.toguzkorgool.dto.response;

import java.util.List;

public record MoveHistoryResponse(List<MoveRecord> moves, int page, int totalPages) {
}
