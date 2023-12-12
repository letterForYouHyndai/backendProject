package site.letterforyou.spring.letter.service;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.log4j.Log4j;
import site.letterforyou.spring.common.domain.PageVO;
import site.letterforyou.spring.common.domain.Pagination;
import site.letterforyou.spring.common.dto.ResponseSuccessDTO;
import site.letterforyou.spring.common.util.ResponseUtil;
import site.letterforyou.spring.common.util.TimeService;
import site.letterforyou.spring.letter.domain.LetterDTO;
import site.letterforyou.spring.letter.domain.LetterVO;
import site.letterforyou.spring.letter.dto.LetterDeleteLetterResponseDTO;
import site.letterforyou.spring.letter.dto.LetterGetLetterResponseDTO;
import site.letterforyou.spring.letter.dto.LetterGetListResponseDTO;
import site.letterforyou.spring.letter.dto.Letterdtos;
import site.letterforyou.spring.letter.mapper.LetterMapper;
import site.letterforyou.spring.common.service.CommonService;

@Service
@Log4j
public class LetterServiceImpl implements LetterService {

    @Autowired
	ResponseUtil responseUtil;
	
	@Autowired
	TimeService timeService;
	
	@Autowired
	CommonService CommonService;

	@Autowired
	private LetterMapper letterMapper;

	@Override
	public ResponseSuccessDTO<LetterDTO> insertLetter(LetterDTO result) {
		
		String url="";
		
		// result.setLetterReceiveId("user1");
		// result.setLetterSendId("user1"); //이후에 확인 후 제거
		// result.setLetterTitle("title");
		// result.setLetterContent("content");
		// result.setKakaoSendYn("N");
		// result.setLetterColorNo("46");
		// result.setLetterReceiveYn("2");
		
		letterMapper.insertLetter(result);
		
		String letterNo = letterMapper.selectLastInsertKey(result);
		String encryptNo="";
		log.info(result.toString());
		//이후에 호스팅 주소로 변경
		try {
			encryptNo = CommonService.encrypt(letterNo);
			log.info("letterNo: "+ letterNo);
		} catch (Exception e) {
			log.info("암호화 중 오류 발생"+ e.getMessage());
		}
		String URL ="http://localhost:8081/api/letter/receive/"+encryptNo;
		
		//String URL ="https://letter4u.site/letter/receive/"+letterNo;
		log.info(result.toString());
		result.setLetterUrl(URL);
		result.setLetterNo(letterNo);
		letterMapper.updateURL(result);
		
		return  responseUtil.successResponse(result, HttpStatus.OK);
		
	}

	@Override
	public String sendKaoKaoMessage(LetterDTO ldto) {

	    String requestURL = "https://kapi.kakao.com/v2/api/talk/memo/default/send";

	    try {
	        URL url = new URL(requestURL);
	        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

	        conn.setRequestMethod("POST");
	        conn.setDoOutput(true);

	        // Access Token 설정
	        conn.setRequestProperty("Authorization", "Bearer " + ldto.getAccessToken());

	        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

	        // 메시지 내용 및 링크 수정
	        JSONObject linkObj = new JSONObject();
	        linkObj.put("web_url", "https://developers.kakao.com");
	        linkObj.put("mobile_web_url", "https://developers.kakao.com");
	        linkObj.put("android_execution_params", "contentId=100");
	        linkObj.put("ios_execution_params", "contentId=100");

	        JSONObject templateObj = new JSONObject();
	        templateObj.put("object_type", "text");
	        templateObj.put("content", new JSONObject()
	            .put("title", ldto.getLetterTitle())
	            .put("description",ldto.getLetterContent())
	            .put("link", linkObj)
	        );

	        String messageData = "template_object=" + templateObj.toString();

	        DataOutputStream outputStream = new DataOutputStream(conn.getOutputStream());
	        outputStream.writeBytes(messageData);
	        outputStream.flush();
	        outputStream.close();

	        int responseCode = conn.getResponseCode();

	        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	        String line;
	        StringBuilder result = new StringBuilder();

	        while ((line = bufferedReader.readLine()) != null) {
	            result.append(line);
	        }

	        ObjectMapper mapper = new ObjectMapper();
	        Map<String, Object> returnMap = mapper.readValue(result.toString(), Map.class);

	    } catch (Exception e) {
	        log.error("카카오 API 메시지 전송 요청 중 에러: " + e.getMessage());
	    }
	    return null;
	}

	@Override
	public String makeURL(LetterDTO ldto) {
		
		
		
		return null;
	}


	@Override
	public ResponseSuccessDTO<LetterGetListResponseDTO> getLetterReceiveList(Long page, String userId) {
		LetterGetListResponseDTO result = new LetterGetListResponseDTO();
		PageVO pageVo = new PageVO(page, 10L, 10L);
		Long offset = pageVo.getOffset();
		Long size = pageVo.getRecordSize();
		log.info(page+" "+ userId+" " +offset+" "+size);
		List<LetterVO> letterVoList = letterMapper.getReceivedLetters(userId, offset, size);

		List<Long> letterList = new ArrayList<>();
		for (LetterVO l : letterVoList) {
			

			letterList.add(l.getLetterNo());
		}
		
		int count = letterMapper.getTotalCountReceivedLetterByUserId(userId);
		Pagination pagination = new Pagination(count, pageVo);
		
		result.setLetterList(letterList);
		result.setPagination(pagination);
		ResponseSuccessDTO<LetterGetListResponseDTO> res = responseUtil.successResponse(result, HttpStatus.OK);

		return res;
	}
	
	@Override
	public ResponseSuccessDTO<LetterGetLetterResponseDTO> getReceivedLetter(Long letterNo) {
		LetterVO letterVo = letterMapper.getReceivedLetter(letterNo);
		LetterGetLetterResponseDTO result = new LetterGetLetterResponseDTO();
		Letterdtos letterDTO = new Letterdtos();
		letterDTO.setLetterNo(letterVo.getLetterNo());
		letterDTO.setLetterTitle(letterVo.getLetterTitle());
		letterDTO.setLetterContent(letterVo.getLetterContent());
		letterDTO.setLetterColorNo(letterVo.getLetterColorNo());
		letterDTO.setReceiverNickname(letterVo.getReceiverNickname());
		letterDTO.setSenderNickname(letterVo.getSenderNickname());
		letterDTO.setRegistDate(timeService.parseLocalDateTimeForLetter(letterVo.getRegistDate()));
		result.setLetterDTO(letterDTO);
		ResponseSuccessDTO<LetterGetLetterResponseDTO> res = responseUtil.successResponse(result, HttpStatus.OK);

		return res;
	}
	
	@Override
	public ResponseSuccessDTO<LetterDeleteLetterResponseDTO> deleteReceivedLetter(Long letterNo) {
		letterMapper.deleteReceivedLetter(letterNo);
		ResponseSuccessDTO<LetterDeleteLetterResponseDTO> res = responseUtil.successResponse("받은 편지"+  letterNo+ " 번이 삭제되었습니다.", HttpStatus.OK);

		return res;
	}

	@Override
	public ResponseSuccessDTO<LetterGetListResponseDTO> getLetterSendList(Long page, String userId) {
		LetterGetListResponseDTO result = new LetterGetListResponseDTO();
		PageVO pageVo = new PageVO(page, 10L, 10L);
		Long offset = pageVo.getOffset();
		Long size = pageVo.getRecordSize();
		log.info(page+" "+ userId+" " +offset+" "+size);
		List<LetterVO> letterVoList = letterMapper.getSendLetters(userId, offset, size);

		List<Long> letterList = new ArrayList<>();
		for (LetterVO l : letterVoList) {
			

			letterList.add(l.getLetterNo());
		}
		
		int count = letterMapper.getTotalCountSendLetterByUserId(userId);
		Pagination pagination = new Pagination(count, pageVo);
		
		result.setLetterList(letterList);
		result.setPagination(pagination);
		ResponseSuccessDTO<LetterGetListResponseDTO> res = responseUtil.successResponse(result, HttpStatus.OK);

		return res;
	}

	@Override
	public ResponseSuccessDTO<LetterGetLetterResponseDTO> getSendLetter(Long letterNo) {
		LetterVO letterVo = letterMapper.getSendLetter(letterNo);
		LetterGetLetterResponseDTO result = new LetterGetLetterResponseDTO();
		Letterdtos letterDTO = new Letterdtos();
		letterDTO.setLetterNo(letterVo.getLetterNo());
		letterDTO.setLetterTitle(letterVo.getLetterTitle());
		letterDTO.setLetterContent(letterVo.getLetterContent());
		letterDTO.setLetterColorNo(letterVo.getLetterColorNo());
		letterDTO.setReceiverNickname(letterVo.getReceiverNickname());
		letterDTO.setSenderNickname(letterVo.getSenderNickname());
		letterDTO.setRegistDate(timeService.parseLocalDateTimeForLetter(letterVo.getRegistDate()));
		result.setLetterDTO(letterDTO);
		ResponseSuccessDTO<LetterGetLetterResponseDTO> res = responseUtil.successResponse(result, HttpStatus.OK);

		return res;
	}

	@Override
	public ResponseSuccessDTO<LetterDeleteLetterResponseDTO> deleteSendLetter(Long letterNo) {
		letterMapper.deleteSendLetter(letterNo);
		ResponseSuccessDTO<LetterDeleteLetterResponseDTO> res = responseUtil.successResponse("보낸 편지"+  letterNo+ " 번이 삭제되었습니다.", HttpStatus.OK);

		return res;
	}

	

}
