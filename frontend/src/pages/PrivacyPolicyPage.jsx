import { useNavigate } from 'react-router-dom'
import './PrivacyPolicyPage.css'

const EFFECTIVE_DATE = '2026-10-15'
const CONTACT_EMAIL = 'rhghdrms10@gmail.com'

function PrivacyPolicyPage() {
  const navigate = useNavigate()

  return (
    <div className="privacy-page">
      <header className="privacy-header">
        <button type="button" aria-label="뒤로" className="privacy-back-btn" onClick={() => navigate('/')}>
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="M15 5l-7 7 7 7" />
          </svg>
        </button>
      </header>

      <h1>개인정보처리방침</h1>
      <p className="privacy-effective-date">시행일: {EFFECTIVE_DATE}</p>

      <p>
        어디서봄(이하 "서비스")은 별도의 회원가입 없이, 기기별로 발급되는 익명 식별자로 이용자를
        구분합니다. 이 방침은 서비스 이용 과정에서 수집하는 정보와 그 처리 방법을 안내합니다.
      </p>

      <section>
        <h2>1. 수집하는 개인정보 항목</h2>
        <table className="privacy-table">
          <thead>
            <tr>
              <th>항목</th>
              <th>수집 시점</th>
              <th>용도</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>익명 식별자(기기별 UUID), 인증 토큰</td>
              <td>서비스 최초 접속 시 자동 발급</td>
              <td>로그인 없이 이용자를 구분하기 위함</td>
            </tr>
            <tr>
              <td>닉네임</td>
              <td>약속방 참여 시 입력</td>
              <td>약속방 내 본인 표시</td>
            </tr>
            <tr>
              <td>출발역, 귀가역, 저장한 역</td>
              <td>약속방 참여, 마이페이지 설정 시 입력</td>
              <td>중간지점·막차 계산</td>
            </tr>
            <tr>
              <td>계좌번호, 은행명</td>
              <td>정산 기능에서 결제자가 선택적으로 입력</td>
              <td>정산 금액 안내, 송금 링크 자동입력</td>
            </tr>
            <tr>
              <td>영수증 사진</td>
              <td>정산 항목 추가 시 결제자가 선택적으로 업로드</td>
              <td>항목 이름·금액 자동 인식(1회성 처리 후 저장하지 않음)</td>
            </tr>
          </tbody>
        </table>
        <p>
          서비스는 현재 위치(GPS) 정보를 수집하지 않습니다. 출발역·귀가역은 이용자가 직접 입력한
          값만 사용합니다.
        </p>
      </section>

      <section>
        <h2>2. 개인정보의 보유 및 이용 기간</h2>
        <p>
          이용자가 약속방을 삭제하거나 참여 정보를 삭제하면 해당 정보는 즉시 삭제됩니다.
          마이페이지에서 계정을 삭제하면, 본인이 방장인 약속방은 참여자·투표·정산 기록을 포함해
          전체 삭제되고, 참여자로만 있던 약속방은 본인의 참여 기록만 삭제됩니다. 별도로 정한
          보유 기간 없이, 삭제 요청 즉시 파기합니다.
        </p>
      </section>

      <section>
        <h2>3. 개인정보의 제3자 제공</h2>
        <p>서비스는 이용자의 개인정보를 외부에 판매하거나 제공하지 않습니다.</p>
      </section>

      <section>
        <h2>4. 외부 서비스 연동</h2>
        <ul>
          <li>카카오맵·네이버지도: 길찾기 링크를 열 때 장소명을 전달합니다(앱 외부 이동).</li>
          <li>토스·카카오페이: 송금 링크를 열 때 계좌 정보(토스)를 전달합니다(앱 외부 이동, 서비스가 직접 송금을 처리하지 않음).</li>
          <li>카카오 로컬 API: 확정된 역 주변 음식점·카페 검색에 사용합니다.</li>
          <li>영수증 인식(Gemini API): 업로드한 영수증 이미지를 1회 분석 용도로만 전송하며, 서버·외부 모두 저장하지 않습니다.</li>
        </ul>
      </section>

      <section>
        <h2>5. 이용자의 권리</h2>
        <p>
          이용자는 별도의 절차 없이 서비스 내에서 직접 본인의 정보를 관리할 수 있습니다.
        </p>
        <ul>
          <li>약속방 삭제: 방 화면 상단의 삭제 버튼(방장만 가능)</li>
          <li>참여 정보 삭제: 약속방 나가기</li>
          <li>계정 전체 삭제: 마이페이지 &gt; 계정 삭제</li>
        </ul>
      </section>

      <section>
        <h2>6. 문의처</h2>
        <p>
          개인정보 처리에 관한 문의는 아래 이메일로 연락해 주세요.
          <br />
          {CONTACT_EMAIL}
        </p>
      </section>
    </div>
  )
}

export default PrivacyPolicyPage
