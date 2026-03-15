package com.besthouse.service;

import com.besthouse.dto.MemberDto;
import com.besthouse.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public List<MemberDto> findAll() {
        return memberRepository.findAllByOrderBySortOrderAsc()
                .stream()
                .map(m -> MemberDto.builder()
                        .memberId(m.getMemberId())
                        .displayName(m.getDisplayName())
                        .role(m.getRole())
                        .weight(m.getWeight())
                        .sortOrder(m.getSortOrder())
                        .build())
                .toList();
    }
}
