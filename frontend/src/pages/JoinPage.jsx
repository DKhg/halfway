import { useCallback, useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { deleteRoom, getRoom, updateRoomFairness, updateRoomMeetingDate } from '../api/roomApi'
import { joinRoom, updateMyStation } from '../api/participantApi'
import { getAllStationNames } from '../api/stationApi'
import { getMe, updateHomeStation } from '../api/userApi'
import { getMidpoint, getMidpointWithHome } from '../api/midpointApi'
import {
  cancelVoting,
  confirmCandidate,
  getCandidates,
  startVoting,
  unconfirmPlace,
  unvoteCandidate,
  updateFixedStation,
  voteCandidate,
} from '../api/voteApi'
import {
  addCustomPlace,
  clearPlace,
  confirmPlace,
  deleteCustomPlace,
  getPlaces,
  getPoolSuggestions,
  searchCustomPlaces,
  unvotePlace,
  votePlace,
} from '../api/placeApi'
import { startSettlement } from '../api/settlementApi'
import { getLastTrain } from '../api/lastTrainApi'
import { getLineColor, getLineLabel } from '../utils/subwayLine'
import { formatMeetingDate } from '../utils/date'
import ConfirmModal from '../components/common/ConfirmModal'
import DateCalendarModal from '../components/common/DateCalendarModal'
import Modal from '../components/common/Modal'
import StepIndicator from '../components/common/StepIndicator'
import StationAutocompleteInput from '../components/common/StationAutocompleteInput'
import { ROOM_FLOW_STEPS } from '../utils/roomSteps'
import './JoinPage.css'

const AUTO_REFRESH_INTERVAL_MS = 10000

// 가게 후보 목록 한 번에 보여주는 개수, "더보기" 클릭마다 이만큼씩 더 공개
const PLACE_PAGE_SIZE = 5

// "맨 위로" 버튼을 보여주기 시작하는 스크롤 위치(px)
const SCROLL_TOP_BUTTON_THRESHOLD = 400

// 막차 계산 모달의 요일 구분 선택지 (날짜투표가 아직 없어 날짜 대신 요일 종류를 직접 고름)
const LAST_TRAIN_DAY_TYPES = [
  { value: 'WEEKDAY', label: '평일' },
  { value: 'SATURDAY', label: '토요일' },
  { value: 'HOLIDAY', label: '휴일' },
]

const FOOD_SUB_CATEGORY_TABS = [
  { key: 'KOREAN', label: '한식', emoji: '🍚' },
  { key: 'CHINESE', label: '중식', emoji: '🍜' },
  { key: 'JAPANESE', label: '일식', emoji: '🍣' },
  { key: 'WESTERN', label: '양식', emoji: '🍝' },
]

// 음식점 탭에서만 쓰는 주점: 한식·중식 등 요리 종류 줄이 아니라 아래 관리용 탭 줄에 붙여서 보여줌
const FOOD_PUB_TAB = { key: 'PUB', label: '주점', emoji: '🍺' }

const CAFE_SUB_CATEGORY_TABS = [
  { key: 'RECOMMEND', label: '추천', emoji: '☕' },
]

// 카테고리 추천 탭과 분리해서 항상 같은 줄에 붙여두는 관리용 탭
const PLACE_MANAGE_TABS = [
  { key: 'CUSTOM', label: '직접추가', emoji: '🔍' },
  { key: 'POOL', label: '다른 모임에서 추가', emoji: '📍' },
]

const PLACE_MAIN_CATEGORY_TABS = [
  { key: 'FOOD', label: '음식점', emoji: '🍴' },
  { key: 'CAFE', label: '카페', emoji: '☕' },
]

function JoinPage() {
  const navigate = useNavigate()
  const { code } = useParams()

  const [room, setRoom] = useState(null)
  const [loadError, setLoadError] = useState(null)
  const isLoading = room === null && !loadError
  const [isManualRefreshing, setIsManualRefreshing] = useState(false)

  const [midpoint, setMidpoint] = useState(null)
  const [midpointError, setMidpointError] = useState(null)

  const [midpointWithHome, setMidpointWithHome] = useState(null)
  const [midpointWithHomeError, setMidpointWithHomeError] = useState(null)
  const [isLoadingMidpointWithHome, setIsLoadingMidpointWithHome] = useState(false)
  // true면 출발역 후보 대신 귀가역까지 고려한 후보를 보여주는 중 (투표 시작도 이 기준을 따름)
  const [isShowingHomeCandidates, setIsShowingHomeCandidates] = useState(false)
  const [myHomeStationName, setMyHomeStationName] = useState(null)
  const [isHomeStationModalOpen, setIsHomeStationModalOpen] = useState(false)
  const [homeStationModalInput, setHomeStationModalInput] = useState('')
  const [isSavingHomeStationModal, setIsSavingHomeStationModal] = useState(false)
  const [homeStationModalError, setHomeStationModalError] = useState(null)

  const [voteResult, setVoteResult] = useState(null)
  const [voteError, setVoteError] = useState(null)
  const [isStartingVoting, setIsStartingVoting] = useState(false)
  const [isCancelingVoting, setIsCancelingVoting] = useState(false)
  const [pendingVoteIds, setPendingVoteIds] = useState(new Set())
  const [confirmTarget, setConfirmTarget] = useState(null)
  const [isConfirming, setIsConfirming] = useState(false)
  const [isUnconfirming, setIsUnconfirming] = useState(false)
  const [isEditingFixedStation, setIsEditingFixedStation] = useState(false)
  const [fixedStationInput, setFixedStationInput] = useState('')
  const [isSavingFixedStation, setIsSavingFixedStation] = useState(false)

  const [activePlaceCategory, setActivePlaceCategory] = useState('FOOD')
  const [foodSubCategory, setFoodSubCategory] = useState('KOREAN')
  const [cafeSubCategory, setCafeSubCategory] = useState('RECOMMEND')
  const [foodOptions, setFoodOptions] = useState([])
  const [cafeOptions, setCafeOptions] = useState([])
  const [placeVisibleCounts, setPlaceVisibleCounts] = useState({})
  const [showScrollTop, setShowScrollTop] = useState(false)
  const [isFoodLoading, setIsFoodLoading] = useState(false)
  const [isCafeLoading, setIsCafeLoading] = useState(false)
  const [placesError, setPlacesError] = useState(null)
  const [placeAlertMessage, setPlaceAlertMessage] = useState(null)
  const [pendingPlaceVotes, setPendingPlaceVotes] = useState(new Set())
  const [isConfirmingPlace, setIsConfirmingPlace] = useState(false)
  const [customSearchKeyword, setCustomSearchKeyword] = useState('')
  const [customSearchResults, setCustomSearchResults] = useState([])
  const [isSearchingCustom, setIsSearchingCustom] = useState(false)
  const [pendingCustomAdds, setPendingCustomAdds] = useState(new Set())
  const [pendingCustomDeletes, setPendingCustomDeletes] = useState(new Set())
  const [deleteConfirmTarget, setDeleteConfirmTarget] = useState(null)
  const [isClearingPlace, setIsClearingPlace] = useState(false)
  const [addCommentTarget, setAddCommentTarget] = useState(null)
  const [addCommentValue, setAddCommentValue] = useState('')
  const [poolSuggestions, setPoolSuggestions] = useState([])

  const [nickname, setNickname] = useState('')
  // 마이페이지에 기본 닉네임이 저장돼 있으면 true — 참여 화면에서 닉네임 입력칸 자체를 안 보여줌
  const [hasProfileNickname, setHasProfileNickname] = useState(false)
  const [station, setStation] = useState('')
  const [homeStation, setHomeStation] = useState('')
  // 체크 시 귀가역 입력칸을 출발역 값과 동기화(비활성화)함. 기본은 해제 — 이미 저장된 귀가역이 있으면 아래 effect가 미리 채워줌
  const [isHomeSameAsDeparture, setIsHomeSameAsDeparture] = useState(false)
  // 마이페이지에 이미 저장된 귀가역 — 입력칸에 미리 채워줌
  const [savedHomeStationName, setSavedHomeStationName] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [actionError, setActionError] = useState(null)
  const [copied, setCopied] = useState(false)
  const [isDeleteConfirmOpen, setIsDeleteConfirmOpen] = useState(false)
  const [isStartSettlementConfirmOpen, setIsStartSettlementConfirmOpen] = useState(false)
  const [isEditingStation, setIsEditingStation] = useState(false)
  const [isChangingFairness, setIsChangingFairness] = useState(false)
  const [isChangingMeetingDate, setIsChangingMeetingDate] = useState(false)
  const [isDateModalOpen, setIsDateModalOpen] = useState(false)

  const [isLastTrainModalOpen, setIsLastTrainModalOpen] = useState(false)
  const [lastTrainStation, setLastTrainStation] = useState('')
  const [lastTrainDayType, setLastTrainDayType] = useState('WEEKDAY')
  const [lastTrainResult, setLastTrainResult] = useState(null)
  const [isCalculatingLastTrain, setIsCalculatingLastTrain] = useState(false)
  const [lastTrainError, setLastTrainError] = useState(null)

  const [stationOptions, setStationOptions] = useState([])
  const [stationNamesLoaded, setStationNamesLoaded] = useState(false)

  useEffect(() => {
    getAllStationNames()
      .then(setStationOptions)
      .catch(() => {})
      .finally(() => setStationNamesLoaded(true))
  }, [])

  // 마이페이지에 저장된 기본 닉네임이 있으면 참여 입력칸 자체를 없애고 그 값으로 바로 참여할 수 있게 함
  useEffect(() => {
    getMe()
      .then((data) => {
        if (data.nickname) {
          setNickname((prev) => prev || data.nickname)
          setHasProfileNickname(true)
        }
        if (data.homeStationName) {
          setSavedHomeStationName(data.homeStationName)
          setHomeStation((prev) => prev || data.homeStationName)
        }
      })
      .catch(() => {})
  }, [])

  // 방 정보를 다시 불러와 참여자 목록·내 상태를 최신화
  const fetchRoom = useCallback(() => {
    return getRoom(code)
      .then((data) => {
        setRoom(data)
        setLoadError(null)
      })
      .catch((err) => {
        setLoadError(err.response?.status === 404 ? '존재하지 않는 방이에요.' : '방 정보를 불러오지 못했어요.')
      })
  }, [code])

  // 중간지점 후보를 다시 계산해서 불러옴
  const fetchMidpoint = useCallback(() => {
    return getMidpoint(code)
      .then((data) => {
        setMidpoint(data)
        setMidpointError(null)
      })
      .catch(() => {
        setMidpointError('중간지점 결과를 불러오지 못했어요.')
      })
  }, [code])

  // 왕복 기준 후보를 불러옴 (귀가역은 모달에서 이미 저장한 뒤 호출됨)
  const handleFetchMidpointWithHome = async () => {
    if (isLoadingMidpointWithHome) {
      return
    }
    setIsLoadingMidpointWithHome(true)
    setMidpointWithHomeError(null)
    try {
      const data = await getMidpointWithHome(code)
      setMidpointWithHome(data)
      setIsShowingHomeCandidates(true)
    } catch {
      setMidpointWithHomeError('귀가역까지 고려한 결과를 불러오지 못했어요.')
    } finally {
      setIsLoadingMidpointWithHome(false)
    }
  }

  // "출발역 기준으로 돌아가기": 귀가역 고려 결과를 버리고 원래 출발역 기준 후보로 돌아감
  const handleBackToDepartureCandidates = () => {
    setIsShowingHomeCandidates(false)
    setMidpointWithHome(null)
    setMidpointWithHomeError(null)
  }

  // "귀가역까지 고려해서 보기" 버튼: 마이페이지에 이미 설정해둔 귀가역이 있으면 모달 없이 바로 그 값으로 조회하고,
  // 없으면 빈칸인 모달을 띄워서 먼저 저장하게 함
  const handleClickConsiderHome = async () => {
    setHomeStationModalError(null)
    try {
      const myInfo = await getMe()
      if (myInfo.homeStationName) {
        setMyHomeStationName(myInfo.homeStationName)
        await handleFetchMidpointWithHome()
        return
      }
      setHomeStationModalInput('')
    } catch {
      setHomeStationModalInput('')
    }
    setIsHomeStationModalOpen(true)
  }

  const handleSaveHomeStationModal = async () => {
    if (isSavingHomeStationModal) {
      return
    }
    setIsSavingHomeStationModal(true)
    setHomeStationModalError(null)
    try {
      await updateHomeStation(homeStationModalInput.trim())
      setMyHomeStationName(homeStationModalInput.trim())
      setIsHomeStationModalOpen(false)
      await handleFetchMidpointWithHome()
    } catch (error) {
      setHomeStationModalError(
        error.response?.status === 400 ? '등록되지 않은 역 이름이에요.' : '저장하지 못했어요. 다시 시도해 주세요.',
      )
    } finally {
      setIsSavingHomeStationModal(false)
    }
  }

  // 투표 후보·득표 현황을 다시 불러옴 (투표 시작 전에는 빈 목록)
  const fetchVote = useCallback(() => {
    return getCandidates(code)
      .then((data) => {
        setVoteResult(data)
        setVoteError(null)
      })
      .catch(() => {
        setVoteError('투표 현황을 불러오지 못했어요.')
      })
  }, [code])

  const refresh = useCallback(
    () => Promise.all([fetchRoom(), fetchMidpoint(), fetchVote()]),
    [fetchRoom, fetchMidpoint, fetchVote],
  )

  useEffect(() => {
    refresh()
  }, [refresh])

  // 화면이 보일 때만 10초마다, 그리고 다시 보이게 된 순간 즉시 재조회
  useEffect(() => {
    const tick = () => {
      if (document.visibilityState === 'visible') {
        refresh()
      }
    }
    const intervalId = setInterval(tick, AUTO_REFRESH_INTERVAL_MS)
    document.addEventListener('visibilitychange', tick)
    return () => {
      clearInterval(intervalId)
      document.removeEventListener('visibilitychange', tick)
    }
  }, [refresh])

  // 가게 후보 목록이 길어질 때만 "맨 위로" 버튼을 보여줌
  useEffect(() => {
    const onScroll = () => setShowScrollTop(window.scrollY > SCROLL_TOP_BUTTON_THRESHOLD)
    window.addEventListener('scroll', onScroll)
    return () => window.removeEventListener('scroll', onScroll)
  }, [])

  // 음식점 탭을 보고 있고 아직 안 골랐으면 선택된 세부 카테고리의 실제 장소 목록을 불러옴
  // room 전체가 아니라 필요한 값만 의존성으로 둬서, 10초 자동 조회마다 카카오 API를 다시 부르지 않게 함
  useEffect(() => {
    if (!room || room.status !== 'CONFIRMED' || room.confirmedFoodPlaceName || activePlaceCategory !== 'FOOD'
      || foodSubCategory === 'POOL') {
      return
    }
    const isCustomTab = foodSubCategory === 'CUSTOM'
    setIsFoodLoading(true)
    setPlacesError(null)
    getPlaces(code, 'FOOD', isCustomTab ? null : foodSubCategory, isCustomTab)
      .then(setFoodOptions)
      .catch(() => setPlacesError('장소 목록을 불러오지 못했어요.'))
      .finally(() => setIsFoodLoading(false))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [room?.status, room?.confirmedFoodPlaceName, code, foodSubCategory, activePlaceCategory])

  // 카페 탭을 보고 있고 아직 안 골랐으면 실제 장소 목록을 불러옴
  useEffect(() => {
    if (!room || room.status !== 'CONFIRMED' || room.confirmedCafePlaceName || activePlaceCategory !== 'CAFE'
      || cafeSubCategory === 'POOL') {
      return
    }
    const isCustomTab = cafeSubCategory === 'CUSTOM'
    setIsCafeLoading(true)
    setPlacesError(null)
    getPlaces(code, 'CAFE', null, isCustomTab)
      .then(setCafeOptions)
      .catch(() => setPlacesError('장소 목록을 불러오지 못했어요.'))
      .finally(() => setIsCafeLoading(false))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [room?.status, room?.confirmedCafePlaceName, code, cafeSubCategory, activePlaceCategory])

  // "다른 모임에서 추가" 탭을 보고 있으면 같은 역의 가게 풀을 불러옴 (득표·확정까지 바로 가능한 후보로 취급)
  useEffect(() => {
    if (!room || room.status !== 'CONFIRMED') {
      return
    }
    const confirmedName = activePlaceCategory === 'FOOD' ? room.confirmedFoodPlaceName : room.confirmedCafePlaceName
    const isPoolTab = activePlaceCategory === 'FOOD' ? foodSubCategory === 'POOL' : cafeSubCategory === 'POOL'
    if (confirmedName || !isPoolTab) {
      return
    }
    getPoolSuggestions(code, activePlaceCategory)
      .then(setPoolSuggestions)
      .catch(() => {})
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [room?.status, room?.confirmedFoodPlaceName, room?.confirmedCafePlaceName, code, foodSubCategory, cafeSubCategory, activePlaceCategory])

  // 정산만 하기 모드는 역·장소 화면이 없어서, 참여하자마자(또는 이미 참여한 채로 재방문하면) 바로 정산 화면으로 보냄
  const settlementOnlyMeId = room?.mode === 'SETTLEMENT_ONLY'
    ? room.participants.find((participant) => participant.me)?.id
    : undefined
  useEffect(() => {
    if (settlementOnlyMeId) {
      handleGoToSettlement()
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [settlementOnlyMeId])

  // 체크·체크 해제 직후 해당 카테고리 목록만 다시 불러와 득표수를 갱신
  const refetchPlaceOptions = async (category) => {
    if (category === 'FOOD') {
      if (foodSubCategory === 'POOL') {
        setPoolSuggestions(await getPoolSuggestions(code, 'FOOD'))
        return
      }
      const isCustomTab = foodSubCategory === 'CUSTOM'
      setFoodOptions(await getPlaces(code, 'FOOD', isCustomTab ? null : foodSubCategory, isCustomTab))
    } else {
      if (cafeSubCategory === 'POOL') {
        setPoolSuggestions(await getPoolSuggestions(code, 'CAFE'))
        return
      }
      const isCustomTab = cafeSubCategory === 'CUSTOM'
      setCafeOptions(await getPlaces(code, 'CAFE', null, isCustomTab))
    }
  }

  const handleConfirmDeleteRoom = async () => {
    setIsDeleteConfirmOpen(false)
    try {
      await deleteRoom(code)
      navigate('/')
    } catch {
      setActionError('삭제하지 못했어요. 다시 시도해 주세요.')
    }
  }

  const handleChangeFairness = async (fairnessType) => {
    if (fairnessType === room.fairnessType || isChangingFairness) {
      return
    }
    setIsChangingFairness(true)
    setActionError(null)
    try {
      await updateRoomFairness(code, fairnessType)
      await refresh()
    } catch {
      setActionError('정렬 기준을 바꾸지 못했어요. 다시 시도해 주세요.')
    } finally {
      setIsChangingFairness(false)
    }
  }

  const handleChangeMeetingDate = async (meetingDate) => {
    if (isChangingMeetingDate) {
      return
    }
    setIsDateModalOpen(false)
    setIsChangingMeetingDate(true)
    setActionError(null)
    try {
      await updateRoomMeetingDate(code, meetingDate)
      await refresh()
    } catch {
      setActionError('약속 날짜를 바꾸지 못했어요. 다시 시도해 주세요.')
    } finally {
      setIsChangingMeetingDate(false)
    }
  }

  const handleStartVoting = async () => {
    if (isStartingVoting) {
      return
    }
    setIsStartingVoting(true)
    setActionError(null)
    try {
      await startVoting(code, isShowingHomeCandidates)
      await refresh()
    } catch {
      setActionError('투표를 시작하지 못했어요. 다시 시도해 주세요.')
    } finally {
      setIsStartingVoting(false)
    }
  }

  const handleCancelVoting = async () => {
    if (isCancelingVoting) {
      return
    }
    setIsCancelingVoting(true)
    setActionError(null)
    try {
      await cancelVoting(code)
      await refresh()
    } catch {
      setActionError('투표를 취소하지 못했어요. 다시 시도해 주세요.')
    } finally {
      setIsCancelingVoting(false)
    }
  }

  const handleToggleVote = async (candidateId, votedByMe) => {
    if (pendingVoteIds.has(candidateId)) {
      return
    }
    setPendingVoteIds((prev) => new Set(prev).add(candidateId))
    setVoteError(null)
    try {
      if (votedByMe) {
        await unvoteCandidate(code, candidateId)
      } else {
        await voteCandidate(code, candidateId)
      }
      await fetchVote()
    } catch {
      setVoteError('체크하지 못했어요. 다시 시도해 주세요.')
    } finally {
      setPendingVoteIds((prev) => {
        const next = new Set(prev)
        next.delete(candidateId)
        return next
      })
    }
  }

  const handleConfirmCandidate = async () => {
    if (!confirmTarget || isConfirming) {
      return
    }
    setIsConfirming(true)
    setVoteError(null)
    try {
      await confirmCandidate(code, confirmTarget.id)
      setConfirmTarget(null)
      await refresh()
    } catch {
      setVoteError('확정하지 못했어요. 다시 시도해 주세요.')
    } finally {
      setIsConfirming(false)
    }
  }

  const handleUnconfirm = async () => {
    if (isUnconfirming) {
      return
    }
    setIsUnconfirming(true)
    setActionError(null)
    try {
      await unconfirmPlace(code)
      await refresh()
    } catch (error) {
      setActionError(error.response?.data?.message || '확정을 취소하지 못했어요. 다시 시도해 주세요.')
    } finally {
      setIsUnconfirming(false)
    }
  }

  const handleStartEditFixedStation = () => {
    setFixedStationInput(room.confirmedStationName)
    setActionError(null)
    setIsEditingFixedStation(true)
  }

  const handleSaveFixedStation = async () => {
    const trimmed = fixedStationInput.trim()
    if (!trimmed || isSavingFixedStation) {
      return
    }
    setIsSavingFixedStation(true)
    setActionError(null)
    try {
      await updateFixedStation(code, trimmed)
      setIsEditingFixedStation(false)
      await refresh()
    } catch (error) {
      setActionError(error.response?.data?.message || '역을 수정하지 못했어요. 다시 시도해 주세요.')
    } finally {
      setIsSavingFixedStation(false)
    }
  }

  const handleTogglePlaceVote = async (category, placeId, votedByMe) => {
    const key = `${category}:${placeId}`
    if (pendingPlaceVotes.has(key)) {
      return
    }
    setPendingPlaceVotes((prev) => new Set(prev).add(key))
    setPlacesError(null)
    try {
      if (votedByMe) {
        await unvotePlace(code, category, placeId)
      } else {
        await votePlace(code, category, placeId)
      }
      await refetchPlaceOptions(category)
      setPoolSuggestions(await getPoolSuggestions(code, category))
    } catch {
      setPlacesError('체크하지 못했어요. 다시 시도해 주세요.')
    } finally {
      setPendingPlaceVotes((prev) => {
        const next = new Set(prev)
        next.delete(key)
        return next
      })
    }
  }

  const handleSearchCustomPlaces = async (category) => {
    const keyword = customSearchKeyword.trim()
    if (!keyword || isSearchingCustom) {
      return
    }
    setIsSearchingCustom(true)
    setPlacesError(null)
    try {
      setCustomSearchResults(await searchCustomPlaces(code, category, keyword))
    } catch {
      setPlacesError('검색하지 못했어요. 다시 시도해 주세요.')
    } finally {
      setIsSearchingCustom(false)
    }
  }

  const handleAddCustomPlace = async (category, place, comment) => {
    if (pendingCustomAdds.has(place.id)) {
      return
    }
    setPendingCustomAdds((prev) => new Set(prev).add(place.id))
    setPlacesError(null)
    try {
      await addCustomPlace(code, {
        category,
        placeId: place.id,
        placeName: place.name,
        roadAddressName: place.roadAddressName,
        placeUrl: place.placeUrl,
        walkMinutes: place.walkMinutes,
        comment: comment?.trim() || undefined,
      })
      setCustomSearchResults((prev) => prev.filter((result) => result.id !== place.id))
      await refetchPlaceOptions(category)
    } catch (error) {
      setPlacesError(error.response?.data?.message || '추가하지 못했어요. 다시 시도해 주세요.')
    } finally {
      setPendingCustomAdds((prev) => {
        const next = new Set(prev)
        next.delete(place.id)
        return next
      })
    }
  }

  // "+ 후보로 추가" 클릭 시 한줄평 입력 모달을 염
  const handleOpenAddCommentModal = (category, place) => {
    setAddCommentValue('')
    setAddCommentTarget({ category, place })
  }

  const handleConfirmAddWithComment = async () => {
    if (!addCommentTarget) {
      return
    }
    const { category, place } = addCommentTarget
    setAddCommentTarget(null)
    await handleAddCustomPlace(category, place, addCommentValue)
  }

  const handleDeleteCustomPlace = async (category, placeId) => {
    if (pendingCustomDeletes.has(placeId)) {
      return
    }
    setPendingCustomDeletes((prev) => new Set(prev).add(placeId))
    setPlacesError(null)
    try {
      await deleteCustomPlace(code, category, placeId)
      await refetchPlaceOptions(category)
    } catch {
      setPlacesError('삭제하지 못했어요. 다시 시도해 주세요.')
    } finally {
      setPendingCustomDeletes((prev) => {
        const next = new Set(prev)
        next.delete(placeId)
        return next
      })
    }
  }

  const handleConfirmDeleteCustomPlace = async () => {
    if (!deleteConfirmTarget) {
      return
    }
    const { category, placeId } = deleteConfirmTarget
    setDeleteConfirmTarget(null)
    await handleDeleteCustomPlace(category, placeId)
  }

  const handleConfirmPlace = async (category, placeName) => {
    if (isConfirmingPlace) {
      return
    }
    setIsConfirmingPlace(true)
    setPlacesError(null)
    try {
      await confirmPlace(code, category, placeName)
      await refresh()
    } catch (error) {
      setPlaceAlertMessage(error.response?.data?.message || '확정하지 못했어요. 다시 시도해 주세요.')
    } finally {
      setIsConfirmingPlace(false)
    }
  }

  const handleClearPlace = async (category) => {
    if (isClearingPlace) {
      return
    }
    setIsClearingPlace(true)
    setPlacesError(null)
    try {
      await clearPlace(code, category)
      await refresh()
    } catch {
      setPlacesError('취소하지 못했어요. 다시 시도해 주세요.')
    } finally {
      setIsClearingPlace(false)
    }
  }

  // 정산 화면으로 이동. 아직 아무도 시작 안 했으면 누른 사람이 그대로 결제자가 됨 (실수로 눌러도 본인이나 방장이 바로 취소 가능)
  // 정산만 하기 모드 진입 등 순수 이동만 필요한 곳에서 사용 (총무 지정 확인 없이 바로 이동)
  const handleGoToSettlement = () => {
    navigate(`/rooms/${code}/settlement`)
  }

  // "정산하러 가기" 버튼: 정산이 시작 전이면 누르는 사람이 총무가 되므로 확인창을 먼저 띄움.
  // 이미 누군가 시작했으면(StepIndicator의 정산 탭도 이 조건으로 풀림) 확인 없이 바로 이동
  const handleSettlementButtonClick = () => {
    if (room.settlementStarted) {
      navigate(`/rooms/${code}/settlement`)
    } else {
      setIsStartSettlementConfirmOpen(true)
    }
  }

  const handleConfirmStartSettlement = async () => {
    setIsStartSettlementConfirmOpen(false)
    try {
      await startSettlement(code)
    } catch {
      // 확인창이 떠 있는 동안 다른 사람이 먼저 시작했을 수도 있음 — 그 경우도 그냥 조회 화면으로 이동
    }
    navigate(`/rooms/${code}/settlement`)
  }

  // 새로고침 버튼 클릭 시 아이콘이 도는 동안 실제로 갱신 중임을 보여줌 (전역 로딩바와 별개로 눌렀다는 즉각적인 피드백)
  const handleManualRefresh = async () => {
    if (isManualRefreshing) {
      return
    }
    setIsManualRefreshing(true)
    try {
      await refresh()
    } finally {
      setIsManualRefreshing(false)
    }
  }

  const handleCopyLink = async () => {
    try {
      await navigator.clipboard.writeText(window.location.href)
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    } catch {
      setCopied(false)
    }
  }

  // 계정에 저장해둔 귀가역이 있으면 미리 채워줌 (사용자가 입력칸을 직접 건드리면 그 값을 그대로 둠)
  const handleOpenLastTrainModal = async () => {
    setIsLastTrainModalOpen(true)
    try {
      const me = await getMe()
      if (me.homeStationName) {
        setLastTrainStation(me.homeStationName)
      }
    } catch {
      // 귀가역 조회 실패해도 수동 입력으로 계속 쓸 수 있어서 조용히 무시
    }
  }

  const handleCloseLastTrainModal = () => {
    setIsLastTrainModalOpen(false)
    setLastTrainStation('')
    setLastTrainDayType('WEEKDAY')
    setLastTrainResult(null)
    setLastTrainError(null)
  }

  const handleCalculateLastTrain = async () => {
    if (!lastTrainStation.trim() || isCalculatingLastTrain) {
      return
    }
    setIsCalculatingLastTrain(true)
    setLastTrainError(null)
    try {
      const result = await getLastTrain(code, lastTrainStation.trim(), lastTrainDayType)
      setLastTrainResult(result)
    } catch (error) {
      setLastTrainError(error.response?.data?.message || '막차를 계산하지 못했어요. 다시 시도해 주세요.')
    } finally {
      setIsCalculatingLastTrain(false)
    }
  }

  const handleJoin = async () => {
    if (!nickname.trim() || isSubmitting) {
      return
    }
    setIsSubmitting(true)
    setActionError(null)
    try {
      await joinRoom(code, nickname.trim())
      await refresh()
    } catch (error) {
      setActionError(error.response?.data?.message || '참여하지 못했어요. 다시 시도해 주세요.')
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleSubmitStation = async () => {
    const trimmedHome = isHomeSameAsDeparture ? station.trim() : homeStation.trim()
    if (!station.trim() || !trimmedHome || isSubmitting) {
      return
    }
    setIsSubmitting(true)
    setActionError(null)
    try {
      await Promise.all([
        updateMyStation(code, { startStationName: station.trim() }),
        updateHomeStation(trimmedHome),
      ])
      setIsEditingStation(false)
      await refresh()
    } catch {
      setActionError('저장하지 못했어요. 다시 시도해 주세요.')
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleStartEditStation = (participant) => {
    setStation(participant.startStationName)
    setIsHomeSameAsDeparture(!savedHomeStationName || savedHomeStationName === participant.startStationName)
    setHomeStation(savedHomeStationName)
    setActionError(null)
    setIsEditingStation(true)
  }

  const handleCancelEditStation = () => {
    setIsEditingStation(false)
    setActionError(null)
  }

  if (isLoading) {
    return (
      <div className="join-page">
        <p className="join-status">불러오는 중...</p>
      </div>
    )
  }

  if (loadError) {
    return (
      <div className="join-page">
        <header className="join-header">
          <button type="button" aria-label="뒤로" className="join-back-btn" onClick={() => navigate('/')}>
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <path d="M15 5l-7 7 7 7" />
            </svg>
          </button>
        </header>
        <p className="join-status">{loadError}</p>
      </div>
    )
  }

  // 백엔드 Jackson 직렬화가 boolean getter의 is 접두사를 벗겨서 me/owner 키로 내려줌
  const me = room.participants.find((participant) => participant.me)
  const submittedOthers = room.participants.filter((participant) => participant.startStationName && !participant.me)
  const doneCount = room.participants.filter((participant) => participant.startStationName).length
  const totalCount = room.participants.length

  // 역 목록을 못 가져왔으면 자동완성 없이도 입력할 수 있게 검증을 건너뜀
  const isKnownStationName = (name) => !stationNamesLoaded || stationOptions.length === 0
    || stationOptions.some((option) => option.name === name)

  const trimmedStation = station.trim()
  const isStationValid = isKnownStationName(trimmedStation)
  const trimmedHomeStation = isHomeSameAsDeparture ? trimmedStation : homeStation.trim()
  const isHomeStationValid = isKnownStationName(trimmedHomeStation)

  // 역 이름으로 노선 배지 목록을 만듦 (환승역은 노선별로 중복 제거)
  const renderLineBadges = (name) => {
    const lines = stationOptions.find((option) => option.name === name)?.lines ?? []
    const deduped = [...new Map(lines.map((line) => [getLineLabel(line), line])).entries()]
    return deduped.map(([label, line]) => (
      <span key={label} className="join-line-badge" style={{ backgroundColor: getLineColor(line) }}>
        {label}
      </span>
    ))
  }

  // 카카오맵·네이버지도 검색 링크 생성
  const buildMapLinks = (placeName) => ({
    kakao: `https://map.kakao.com/link/search/${encodeURIComponent(`${room.confirmedStationName}역 ${placeName}`)}`,
    naver: `https://map.naver.com/v5/search/${encodeURIComponent(`${room.confirmedStationName}역 ${placeName}`)}`,
  })

  // 음식점·카페 모두 확정된 뒤 보여줄 요약 카드 (탭 전환 없이 한 번에 둘 다 보여줌)
  const renderConfirmedSummaryCard = (category, label, placeName) => {
    const links = buildMapLinks(placeName)
    return (
      <div
        key={category}
        className={`join-confirmed-summary-card join-confirmed-summary-card--${category.toLowerCase()}`}
      >
        <div className="join-confirmed-summary-head">
          <div className="join-confirmed-summary-head-top">
            <span className="join-confirmed-summary-label">{label}</span>
            <span className="join-confirmed-summary-badge">확정</span>
          </div>
          <span className="join-confirmed-summary-name">{placeName}</span>
        </div>
        <div className="join-confirmed-summary-actions">
          <a className="join-confirmed-summary-link" href={links.kakao} target="_blank" rel="noreferrer">
            카카오맵
          </a>
          <a className="join-confirmed-summary-link" href={links.naver} target="_blank" rel="noreferrer">
            네이버지도
          </a>
          {room.owner && (
            <button
              type="button"
              className="join-confirmed-summary-clear"
              disabled={isClearingPlace}
              onClick={() => handleClearPlace(category)}
            >
              확정 취소
            </button>
          )}
        </div>
      </div>
    )
  }

  // 음식점·카페 한 카테고리의 확정 결과 또는 투표 목록을 렌더링
  const renderPlaceCategory = (category, confirmedName, options, isOptionsLoading, subCategoryTabs,
    activeSubCategory, onSubCategoryChange) => {
    const isCustomTab = activeSubCategory === 'CUSTOM'
    const isPoolTab = activeSubCategory === 'POOL'
    const displayedOptions = isPoolTab ? poolSuggestions : options
    const pageKey = `${category}:${activeSubCategory}`
    const visibleCount = placeVisibleCounts[pageKey] ?? PLACE_PAGE_SIZE
    const pagedOptions = displayedOptions.slice(0, visibleCount)
    const hasMoreOptions = displayedOptions.length > visibleCount
    return (
    <div className="join-confirmed-place-section">
      {confirmedName ? (
        renderConfirmedSummaryCard(category, category === 'FOOD' ? '음식점' : '카페', confirmedName)
      ) : (
        <div className="join-place-picker">
          <div className="join-place-category-frame">
            <span className="join-place-category-label">카테고리</span>

            {category === 'CAFE' ? (
              // 카페는 하위 카테고리가 "추천" 하나뿐이라 관리용 탭까지 한 줄에 붙임
              <div className="join-place-tabs join-place-sub-tabs">
                {[...subCategoryTabs, ...PLACE_MANAGE_TABS].map((tab) => (
                  <button
                    key={tab.key}
                    type="button"
                    aria-pressed={tab.key === activeSubCategory}
                    className="join-place-sub-tab-btn"
                    onClick={() => onSubCategoryChange(tab.key)}
                  >
                    {tab.emoji} {tab.label}
                  </button>
                ))}
              </div>
            ) : (
              <>
                <div className="join-place-tabs join-place-sub-tabs">
                  {subCategoryTabs.map((tab) => (
                    <button
                      key={tab.key}
                      type="button"
                      aria-pressed={tab.key === activeSubCategory}
                      className="join-place-sub-tab-btn"
                      onClick={() => onSubCategoryChange(tab.key)}
                    >
                      {tab.emoji} {tab.label}
                    </button>
                  ))}
                </div>

                <div className="join-place-tabs join-place-sub-tabs">
                  {/* 음식점 탭에서만 주점을 직접추가·다른 모임에서 추가와 같은 줄에 붙임 */}
                  {[FOOD_PUB_TAB, ...PLACE_MANAGE_TABS].map((tab) => (
                    <button
                      key={tab.key}
                      type="button"
                      aria-pressed={tab.key === activeSubCategory}
                      className="join-place-sub-tab-btn"
                      onClick={() => onSubCategoryChange(tab.key)}
                    >
                      {tab.emoji} {tab.label}
                    </button>
                  ))}
                </div>
              </>
            )}
          </div>

          {isOptionsLoading && <div className="join-progress-hint">불러오는 중...</div>}
          {!isOptionsLoading && displayedOptions.length === 0 && (
            <div className="join-progress-hint">
              {isCustomTab && '아직 직접 추가한 곳이 없어요.'}
              {isPoolTab && '같은 역에서 다른 모임이 추가한 곳이 아직 없어요.'}
              {!isCustomTab && !isPoolTab && '이 카테고리엔 아직 후보가 없어요.'}
            </div>
          )}

          <div className="join-place-list">
            {pagedOptions.map((place) => {
              const key = `${category}:${place.id}`
              const meta = [place.roadAddressName, place.walkMinutes ? `도보 약 ${place.walkMinutes}분` : null]
                .filter(Boolean)
                .join(' · ')
              return (
                <div key={place.id} className="join-place-card">
                  <div className="join-place-name-row">
                    <div className="join-place-name">{place.name}</div>
                    {place.custom && !isPoolTab && <span className="join-place-custom-badge">직접 추가</span>}
                    {place.custom && !isPoolTab && (place.addedByMe || room.owner) && (
                      <button
                        type="button"
                        className="join-place-delete-btn"
                        disabled={pendingCustomDeletes.has(place.id)}
                        onClick={() => setDeleteConfirmTarget({ category, placeId: place.id, placeName: place.name })}
                      >
                        삭제
                      </button>
                    )}
                  </div>
                  {meta && <div className="join-place-meta">{meta}</div>}
                  {place.comment && (
                    <div className="join-place-comment">
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                        <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z" />
                      </svg>
                      {place.comment}
                    </div>
                  )}
                  <div className="join-place-card-actions">
                    <button
                      type="button"
                      aria-pressed={place.votedByMe}
                      aria-label={place.votedByMe ? '좋아요 취소' : '좋아요'}
                      className="join-place-like-btn"
                      disabled={pendingPlaceVotes.has(key)}
                      onClick={() => handleTogglePlaceVote(category, place.id, place.votedByMe)}
                    >
                      <svg
                        width="18"
                        height="18"
                        viewBox="0 0 24 24"
                        fill={place.votedByMe ? 'currentColor' : 'none'}
                        stroke="currentColor"
                        strokeWidth="2"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        aria-hidden="true"
                      >
                        <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
                      </svg>
                      <span>{place.voteCount}</span>
                    </button>
                    {place.placeUrl && (
                      <a
                        className="join-place-kakao-link"
                        href={place.placeUrl}
                        target="_blank"
                        rel="noreferrer"
                      >
                        카카오맵에서 보기
                      </a>
                    )}
                    {room.owner && (
                      <button
                        type="button"
                        className="join-place-choose-btn"
                        disabled={isConfirmingPlace}
                        onClick={() => handleConfirmPlace(category, place.name)}
                      >
                        이 곳으로 확정
                      </button>
                    )}
                  </div>
                </div>
              )
            })}
          </div>

          {hasMoreOptions && (
            <button
              type="button"
              className="join-place-more-btn"
              onClick={() => setPlaceVisibleCounts((prev) => ({ ...prev, [pageKey]: visibleCount + PLACE_PAGE_SIZE }))}
            >
              + 더보기
            </button>
          )}

          {isCustomTab && (
            <div className="join-place-search">
              <div className="join-place-search-hint">확정 역 기준 2km 이내 장소만 검색돼요.</div>
              <div className="join-place-search-row">
                <input
                  type="text"
                  placeholder="가게 이름으로 직접 검색"
                  className="join-place-search-input"
                  value={customSearchKeyword}
                  onChange={(e) => setCustomSearchKeyword(e.target.value)}
                  onKeyDown={(e) => e.key === 'Enter' && handleSearchCustomPlaces(category)}
                />
                <button
                  type="button"
                  className="join-place-search-btn"
                  disabled={!customSearchKeyword.trim() || isSearchingCustom}
                  onClick={() => handleSearchCustomPlaces(category)}
                >
                  {isSearchingCustom ? '검색 중...' : '검색'}
                </button>
              </div>

              {customSearchResults.length > 0 && (
                <div className="join-place-search-results">
                  {customSearchResults.map((result) => {
                    const meta = [result.roadAddressName, result.walkMinutes ? `도보 약 ${result.walkMinutes}분` : null]
                      .filter(Boolean)
                      .join(' · ')
                    return (
                      <div key={result.id} className="join-place-search-result-card">
                        <div className="join-place-name">{result.name}</div>
                        {meta && <div className="join-place-meta">{meta}</div>}
                        <div className="join-place-card-actions">
                          {result.placeUrl && (
                            <a
                              className="join-place-kakao-link"
                              href={result.placeUrl}
                              target="_blank"
                              rel="noreferrer"
                            >
                              카카오맵에서 보기
                            </a>
                          )}
                          <button
                            type="button"
                            className="join-place-search-add-btn"
                            disabled={pendingCustomAdds.has(result.id)}
                            onClick={() => handleOpenAddCommentModal(category, result)}
                          >
                            + 후보로 추가
                          </button>
                        </div>
                      </div>
                    )
                  })}
                </div>
              )}
            </div>
          )}
        </div>
      )}
    </div>
    )
  }

  let hint
  if (!me) {
    hint = '참여해서 출발역을 알려주세요'
  } else if (!me.startStationName) {
    hint = `${me.nickname}님 차례예요!`
  } else if (doneCount === totalCount) {
    hint = '모두 입력 완료! 결과 기다리는 중'
  } else {
    hint = '다른 참여자 입력을 기다리는 중'
  }

  const header = (
    <header className="join-header">
      <button type="button" aria-label="뒤로" className="join-back-btn" onClick={() => navigate('/')}>
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
          <path d="M15 5l-7 7 7 7" />
        </svg>
      </button>
      <div className="join-header-actions">
        <button
          type="button"
          aria-label="새로고침"
          aria-busy={isManualRefreshing}
          className="join-refresh-btn"
          disabled={isManualRefreshing}
          onClick={handleManualRefresh}
        >
          <svg
            width="20"
            height="20"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2.4"
            strokeLinecap="round"
            strokeLinejoin="round"
            aria-hidden="true"
            className={isManualRefreshing ? 'join-refresh-icon join-refresh-icon--spinning' : 'join-refresh-icon'}
          >
            <path d="M21 12a9 9 0 1 1-2.64-6.36" />
            <polyline points="21 3 21 9 15 9" />
          </svg>
        </button>
        {room.status === 'CONFIRMED' && (
          <button
            type="button"
            className="join-lasttrain-btn"
            onClick={handleOpenLastTrainModal}
          >
            막차 계산
          </button>
        )}
        <button type="button" className="join-copy-btn" onClick={handleCopyLink}>
          {copied ? '복사됨!' : '링크 복사'}
        </button>
        {room.owner && (
          <button type="button" aria-label="방 삭제" className="join-delete-btn" onClick={() => setIsDeleteConfirmOpen(true)}>
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <path d="M4 7h16M9 7V5a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2m-9 0 1 13a1 1 0 0 0 1 1h8a1 1 0 0 0 1-1l1-13" />
            </svg>
          </button>
        )}
      </div>
    </header>
  )

  const deleteModal = isDeleteConfirmOpen && (
    <ConfirmModal
      title="방 삭제"
      message="정말 이 약속을 삭제할까요? 되돌릴 수 없어요."
      confirmLabel="삭제"
      cancelLabel="취소"
      danger
      onConfirm={handleConfirmDeleteRoom}
      onCancel={() => setIsDeleteConfirmOpen(false)}
    />
  )

  if (room.mode === 'SETTLEMENT_ONLY') {
    return (
      <div className="join-page">
        {header}

        <div className="join-title-block">
          <h1>{room.name}</h1>
          <p className="join-progress-hint">역·장소 확정 없이 정산만 빠르게 하는 방이에요</p>
        </div>

        {!me && (
          <div className="join-my-turn-card">
            <div className="join-my-turn-title">참여하기</div>

            {hasProfileNickname ? (
              <p className="join-progress-hint">&quot;{nickname}&quot;(으)로 참여해요</p>
            ) : (
              <>
                <label htmlFor="join-nickname-input" className="join-field-label">
                  닉네임
                </label>
                <input
                  id="join-nickname-input"
                  type="text"
                  placeholder="닉네임"
                  className="join-station-input"
                  maxLength={20}
                  value={nickname}
                  onChange={(e) => setNickname(e.target.value)}
                />
              </>
            )}

            {actionError && <div className="join-error">{actionError}</div>}

            <button
              type="button"
              className="join-submit-cta"
              disabled={!nickname.trim() || isSubmitting}
              onClick={handleJoin}
            >
              {isSubmitting ? '참여하는 중...' : '참여하기'}
            </button>
          </div>
        )}

        {me && <p className="join-progress-hint">정산 화면으로 이동하는 중...</p>}

        {deleteModal}
      </div>
    )
  }

  if (room.status === 'CONFIRMED') {
    const isPlaceStillConfirmed = Boolean(room.confirmedFoodPlaceName || room.confirmedCafePlaceName)
    const includedCategoryTabs = PLACE_MAIN_CATEGORY_TABS.filter((tab) =>
      tab.key === 'FOOD' ? room.includesFood : room.includesCafe)
    const activeIncludedCategory = includedCategoryTabs.some((tab) => tab.key === activePlaceCategory)
      ? activePlaceCategory
      : includedCategoryTabs[0]?.key
    const allNeededPlacesConfirmed = (!room.includesFood || room.confirmedFoodPlaceName)
      && (!room.includesCafe || room.confirmedCafePlaceName)
    const isFixed = room.mode === 'FIXED'

    return (
      <div className="join-page">
        {header}

        <div className="join-confirmed-block">
          <StepIndicator
            steps={ROOM_FLOW_STEPS}
            currentKey="PLACE"
            disabledKeys={[
              ...(room.settlementStarted ? [] : ['SETTLEMENT']),
              ...(room.owner ? [] : ['STATION']),
            ]}
            onStepClick={(key) => {
              if (key === 'SETTLEMENT') {
                navigate(`/rooms/${code}/settlement`)
              } else if (key === 'STATION') {
                document.getElementById('join-unconfirm-zone')?.scrollIntoView({ behavior: 'smooth', block: 'center' })
              }
            }}
          />

          <h1 className="join-confirmed-station">{room.confirmedStationName}</h1>

          {!me && isFixed && (
            <div className="join-my-turn-card">
              <div className="join-my-turn-title">참여하기</div>

              {hasProfileNickname ? (
                <p className="join-progress-hint">&quot;{nickname}&quot;(으)로 참여해요</p>
              ) : (
                <>
                  <label htmlFor="join-nickname-input" className="join-field-label">
                    닉네임
                  </label>
                  <input
                    id="join-nickname-input"
                    type="text"
                    placeholder="닉네임"
                    className="join-station-input"
                    maxLength={20}
                    value={nickname}
                    onChange={(e) => setNickname(e.target.value)}
                  />
                </>
              )}

              {actionError && <div className="join-error">{actionError}</div>}

              <button
                type="button"
                className="join-submit-cta"
                disabled={!nickname.trim() || isSubmitting}
                onClick={handleJoin}
              >
                {isSubmitting ? '참여하는 중...' : '참여하기'}
              </button>
            </div>
          )}

          {me && (allNeededPlacesConfirmed ? (
            <div className="join-confirmed-summary">
              {room.includesFood && renderConfirmedSummaryCard('FOOD', '음식점', room.confirmedFoodPlaceName)}
              {room.includesCafe && renderConfirmedSummaryCard('CAFE', '카페', room.confirmedCafePlaceName)}
            </div>
          ) : (
            <>
              {includedCategoryTabs.length > 1 && (
                <div className="join-place-tabs">
                  {includedCategoryTabs.map((tab) => (
                    <button
                      key={tab.key}
                      type="button"
                      aria-pressed={tab.key === activeIncludedCategory}
                      className="join-place-main-tab-btn"
                      onClick={() => setActivePlaceCategory(tab.key)}
                    >
                      {tab.emoji} {tab.label}
                    </button>
                  ))}
                </div>
              )}

              {activeIncludedCategory === 'FOOD'
                ? renderPlaceCategory('FOOD', room.confirmedFoodPlaceName, foodOptions, isFoodLoading,
                  FOOD_SUB_CATEGORY_TABS, foodSubCategory, setFoodSubCategory)
                : renderPlaceCategory('CAFE', room.confirmedCafePlaceName, cafeOptions, isCafeLoading,
                  CAFE_SUB_CATEGORY_TABS, cafeSubCategory, setCafeSubCategory)}
            </>
          ))}

          {actionError && <div className="join-error">{actionError}</div>}
          {placesError && <div className="join-error">{placesError}</div>}

          {room.owner && isFixed && (
            <div className="join-unconfirm-zone" id="join-unconfirm-zone">
              {isEditingFixedStation ? (
                <>
                  <StationAutocompleteInput
                    placeholder="역 이름"
                    value={fixedStationInput}
                    onChange={setFixedStationInput}
                    stationOptions={stationOptions}
                  />
                  <div className="join-fixed-station-actions">
                    <button
                      type="button"
                      className="join-unconfirm-btn"
                      disabled={isSavingFixedStation || !fixedStationInput.trim()}
                      onClick={handleSaveFixedStation}
                    >
                      {isSavingFixedStation ? '저장하는 중...' : '저장'}
                    </button>
                    <button
                      type="button"
                      className="join-unconfirm-btn"
                      disabled={isSavingFixedStation}
                      onClick={() => setIsEditingFixedStation(false)}
                    >
                      취소
                    </button>
                  </div>
                </>
              ) : (
                <button
                  type="button"
                  className="join-unconfirm-btn"
                  disabled={isPlaceStillConfirmed}
                  onClick={handleStartEditFixedStation}
                >
                  역 수정
                </button>
              )}
              <p className="join-unconfirm-hint">
                {isPlaceStillConfirmed
                  ? "확정된 음식점·카페에서 '확정 취소' 버튼을 눌러주세요."
                  : '장소를 바로 정한 방이라 투표 없이 역만 바로 수정돼요.'}
              </p>
            </div>
          )}

          {room.owner && !isFixed && (
            <div className="join-unconfirm-zone" id="join-unconfirm-zone">
              <button
                type="button"
                className="join-unconfirm-btn"
                disabled={isUnconfirming || isPlaceStillConfirmed}
                onClick={handleUnconfirm}
              >
                {isUnconfirming ? '취소하는 중...' : '역 확정 취소하고 다시 투표'}
              </button>
              <p className="join-unconfirm-hint">
                {isPlaceStillConfirmed
                  ? "확정된 음식점·카페에서 '확정 취소' 버튼을 눌러주세요."
                  : '역 확정이 취소되고 다시 투표할 수 있어요.'}
              </p>
            </div>
          )}

          {me && (room.confirmedFoodPlaceName || room.confirmedCafePlaceName) && (
            <button
              type="button"
              className="join-settlement-link-btn"
              onClick={handleSettlementButtonClick}
            >
              정산하러 가기
            </button>
          )}
        </div>

        {isStartSettlementConfirmOpen && (
          <ConfirmModal
            message="총무가 되는 거예요, 계속할까요?"
            confirmLabel="계속"
            cancelLabel="취소"
            onConfirm={handleConfirmStartSettlement}
            onCancel={() => setIsStartSettlementConfirmOpen(false)}
          />
        )}

        {placeAlertMessage && (
          <ConfirmModal
            title="확정할 수 없어요"
            message={placeAlertMessage}
            onConfirm={() => setPlaceAlertMessage(null)}
          />
        )}

        {deleteModal}

        {deleteConfirmTarget && (
          <ConfirmModal
            title="직접 추가한 곳 삭제"
            message={`"${deleteConfirmTarget.placeName}"을 후보에서 삭제할까요?`}
            confirmLabel="삭제"
            cancelLabel="취소"
            danger
            onConfirm={handleConfirmDeleteCustomPlace}
            onCancel={() => setDeleteConfirmTarget(null)}
          />
        )}

        {addCommentTarget && (
          <Modal title="한줄평 남기기" onClose={() => setAddCommentTarget(null)}>
            <p className="join-add-comment-place-name">{addCommentTarget.place.name}</p>
            <input
              type="text"
              placeholder="한줄평 (선택, 20자 이내)"
              className="join-place-comment-input"
              maxLength={20}
              autoFocus
              value={addCommentValue}
              onChange={(e) => setAddCommentValue(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handleConfirmAddWithComment()}
            />
            <button
              type="button"
              className="join-add-comment-confirm-btn"
              disabled={pendingCustomAdds.has(addCommentTarget.place.id)}
              onClick={handleConfirmAddWithComment}
            >
              후보로 추가
            </button>
          </Modal>
        )}

        {isLastTrainModalOpen && (
          <Modal title="막차 계산" onClose={handleCloseLastTrainModal}>
            <div className="join-lasttrain-field">
              <span className="join-lasttrain-badge join-lasttrain-badge--from">출발역</span>
              <div className="join-lasttrain-static">{room.confirmedStationName}</div>
            </div>
            <div className="join-lasttrain-field">
              <label htmlFor="join-lasttrain-input" className="join-lasttrain-badge join-lasttrain-badge--to">
                귀가역
              </label>
              <StationAutocompleteInput
                id="join-lasttrain-input"
                placeholder="잠실역 또는 초성(ㅈㅅ)"
                value={lastTrainStation}
                onChange={(value) => {
                  setLastTrainStation(value)
                  setLastTrainResult(null)
                  setLastTrainError(null)
                }}
                stationOptions={stationOptions}
              />
            </div>
            <div className="join-lasttrain-field">
              <span className="join-lasttrain-label">요일</span>
              <div className="join-lasttrain-daytype" role="radiogroup" aria-label="요일 구분">
                {LAST_TRAIN_DAY_TYPES.map((option) => (
                  <button
                    key={option.value}
                    type="button"
                    role="radio"
                    aria-checked={lastTrainDayType === option.value}
                    className={
                      'join-lasttrain-daytype-btn' +
                      (lastTrainDayType === option.value ? ' join-lasttrain-daytype-btn--active' : '')
                    }
                    onClick={() => {
                      setLastTrainDayType(option.value)
                      setLastTrainResult(null)
                      setLastTrainError(null)
                    }}
                  >
                    {option.label}
                  </button>
                ))}
              </div>
            </div>
            <button
              type="button"
              className="join-lasttrain-calc-btn"
              disabled={!lastTrainStation.trim() || isCalculatingLastTrain}
              onClick={handleCalculateLastTrain}
            >
              {isCalculatingLastTrain ? '계산 중...' : '막차 계산'}
            </button>
            {lastTrainError && <div className="join-error">{lastTrainError}</div>}
            {lastTrainResult && lastTrainResult.unsupported && (
              <div className="join-lasttrain-result join-lasttrain-result--muted">
                아직 이 경로는 계산할 수 없어요.
              </div>
            )}
            {lastTrainResult && !lastTrainResult.unsupported && lastTrainResult.lastDepartureTime && (
              <div className="join-lasttrain-result">
                <span className="join-lasttrain-result-label">막차 시각</span>
                <span className="join-lasttrain-result-time">{lastTrainResult.lastDepartureTime}</span>
              </div>
            )}
            {lastTrainResult && !lastTrainResult.unsupported && !lastTrainResult.lastDepartureTime && (
              <div className="join-lasttrain-result join-lasttrain-result--muted">
                이 시간대엔 막차 정보를 찾을 수 없어요.
              </div>
            )}
            <p className="join-lasttrain-caption">지하철 기준이에요. 운행 사정에 따라 달라질 수 있어요.</p>
          </Modal>
        )}

        {showScrollTop && (
          <button
            type="button"
            aria-label="맨 위로"
            className="join-scroll-top-btn"
            onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <path d="M12 19V5M5 12l7-7 7 7" />
            </svg>
          </button>
        )}
      </div>
    )
  }

  return (
    <div className="join-page">
      {header}

      <div className="join-title-block">
        <h1>{room.name}</h1>
        <div className="join-progress-row">
          <span className="join-progress-badge">
            {doneCount}/{totalCount} 입력 완료
          </span>
          <span className="join-progress-hint">{hint}</span>
        </div>
        <div className="join-meeting-date-row">
          <span className="join-meeting-date-text">
            {formatMeetingDate(room.meetingDate) ?? '약속 날짜 미정'}
          </span>
          {(room.owner || me) && !room.settlementStarted && (
            <button
              type="button"
              className="join-meeting-date-edit-btn"
              disabled={isChangingMeetingDate}
              onClick={() => setIsDateModalOpen(true)}
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                <path d="M4 20l1-4.5L16.5 4 20 7.5 8.5 19 4 20z" />
              </svg>
              날짜 수정
            </button>
          )}
        </div>
      </div>

      {isDateModalOpen && (
        <DateCalendarModal
          initialDate={room.meetingDate}
          onSelect={handleChangeMeetingDate}
          onClear={() => handleChangeMeetingDate(null)}
          onClose={() => setIsDateModalOpen(false)}
        />
      )}

      {submittedOthers.length > 0 && (
        <div className="join-participant-scroll">
          {submittedOthers.map((participant) => (
            <div key={participant.id} className="join-participant-chip">
              <span className="join-participant-chip-name">{participant.nickname}</span>
              <span className="join-participant-chip-station">{participant.startStationName}</span>
            </div>
          ))}
        </div>
      )}

      {!me && (
        <div className="join-my-turn-card">
          <div className="join-my-turn-title">참여하기</div>

          {hasProfileNickname ? (
            <p className="join-progress-hint">&quot;{nickname}&quot;(으)로 참여해요</p>
          ) : (
            <>
              <label htmlFor="join-nickname-input" className="join-field-label">
                닉네임
              </label>
              <input
                id="join-nickname-input"
                type="text"
                placeholder="닉네임"
                className="join-station-input"
                maxLength={20}
                value={nickname}
                onChange={(e) => setNickname(e.target.value)}
              />
            </>
          )}

          {actionError && <div className="join-error">{actionError}</div>}

          <button
            type="button"
            className="join-submit-cta"
            disabled={!nickname.trim() || isSubmitting}
            onClick={handleJoin}
          >
            {isSubmitting ? '참여하는 중...' : '참여하기'}
          </button>
        </div>
      )}

      {me && room.status === 'COLLECTING' && (!me.startStationName || isEditingStation) && (
        <>
          <div className="join-my-turn-card">
            <div className="join-my-turn-title">
              {me.startStationName ? '출발역, 귀가역 수정' : `${me.nickname} 차례!`}
            </div>

            <label htmlFor="join-station-input" className="join-field-label">
              출발역
            </label>
            <StationAutocompleteInput
              id="join-station-input"
              placeholder="잠실역 또는 초성(ㅈㅅ)"
              value={station}
              onChange={setStation}
              stationOptions={stationOptions}
            />

            <label htmlFor="join-home-station-input" className="join-field-label">
              귀가역
            </label>
            <StationAutocompleteInput
              id="join-home-station-input"
              placeholder="집에서 제일 가까운 역"
              value={isHomeSameAsDeparture ? station : homeStation}
              onChange={setHomeStation}
              stationOptions={stationOptions}
              disabled={isHomeSameAsDeparture}
            />

            <label className="join-home-same-check">
              <input
                type="checkbox"
                className="join-home-same-check-input"
                checked={isHomeSameAsDeparture}
                onChange={(e) => {
                  const checked = e.target.checked
                  setIsHomeSameAsDeparture(checked)
                  if (!checked && !homeStation) {
                    setHomeStation(savedHomeStationName || station)
                  }
                }}
              />
              <span className="join-home-same-check-box" aria-hidden="true">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M5 13l4 4L19 7" />
                </svg>
              </span>
              귀가역도 출발역과 동일해요
            </label>

            {actionError && <div className="join-error">{actionError}</div>}
          </div>

          <button
            type="button"
            className="join-submit-cta"
            disabled={!trimmedStation || !isStationValid || !trimmedHomeStation || !isHomeStationValid || isSubmitting}
            onClick={handleSubmitStation}
          >
            {isSubmitting ? '저장하는 중...' : me.startStationName ? '수정 완료' : '중간지점 찾기'}
          </button>
          {me.startStationName && (
            <button
              type="button"
              className="join-edit-cancel-btn"
              disabled={isSubmitting}
              onClick={handleCancelEditStation}
            >
              취소
            </button>
          )}
        </>
      )}

      {me && room.status === 'COLLECTING' && me.startStationName && !isEditingStation && (!midpoint || midpoint.status === 'WAITING') && (
        <div className="join-my-turn-card">
          <div className="join-my-turn-title-row">
            <div className="join-my-turn-title">입력 완료!</div>
            <button type="button" className="join-edit-station-btn" onClick={() => handleStartEditStation(me)}>
              수정
            </button>
          </div>
          <div className="join-participant-station">내 출발역: {me.startStationName}</div>
          {midpointError && <div className="join-error">{midpointError}</div>}
          {!midpointError && !midpoint && <div className="join-progress-hint">결과 불러오는 중...</div>}
          {!midpointError && midpoint && midpoint.status === 'WAITING' && (
            <div className="join-progress-hint">
              출발역 입력한 사람이 {midpoint.readyParticipantCount}명이에요. 2명 이상 모이면 중간지점을 보여드려요.
            </div>
          )}
        </div>
      )}

      {me && room.status === 'COLLECTING' && me.startStationName && !isEditingStation && midpoint && midpoint.status === 'READY' && (
        <>
          <div className="join-my-station-card">
            <span>내 출발역: {me.startStationName}</span>
            <button type="button" className="join-edit-station-btn" onClick={() => handleStartEditStation(me)}>
              수정
            </button>
          </div>

          <div className="join-midpoint-home-section">
            {!isShowingHomeCandidates ? (
              <button
                type="button"
                className="join-midpoint-home-btn"
                disabled={isLoadingMidpointWithHome}
                onClick={handleClickConsiderHome}
              >
                {isLoadingMidpointWithHome ? '계산하는 중...' : '귀가역까지 고려해서 보기'}
              </button>
            ) : (
              <button type="button" className="join-midpoint-home-btn" onClick={handleBackToDepartureCandidates}>
                출발역 기준으로 돌아가기
              </button>
            )}
            {midpointWithHomeError && <div className="join-error">{midpointWithHomeError}</div>}
          </div>

          {!isShowingHomeCandidates && (
            midpoint.candidates.length === 0 ? (
              <div className="join-midpoint-empty">
                참여자들 사이 거리가 너무 멀어서 90분 안에 갈 수 있는 역을 찾지 못했어요. 출발역을 다시 확인해 주세요.
              </div>
            ) : (
              <>
                <div className="join-midpoint-list">
                  {midpoint.candidates.map((candidate, index) => (
                    <div key={candidate.stationName} className="join-midpoint-card">
                      <div className="join-midpoint-header">
                        <span className="join-midpoint-rank">{index + 1}위</span>
                        <span className="join-midpoint-name">{candidate.stationName}</span>
                        <span className="join-midpoint-lines">{renderLineBadges(candidate.stationName)}</span>
                        <span className="join-midpoint-fairness">공평도 {candidate.fairnessScore}</span>
                      </div>
                      <div className="join-midpoint-summary">
                        제일 오래 걸리는 사람 {candidate.maxMinutes}분
                      </div>
                      <div className="join-midpoint-times">
                        {candidate.times.map((time) => (
                          <span
                            key={time.participantId}
                            className={
                              time.participantId === me.id
                                ? 'join-midpoint-time-chip join-midpoint-time-chip--me'
                                : 'join-midpoint-time-chip'
                            }
                          >
                            <span className="join-midpoint-time-chip-name">{time.nickname}</span>
                            <span className="join-midpoint-time-chip-value">{time.minutes}분</span>
                          </span>
                        ))}
                      </div>
                    </div>
                  ))}
                </div>
                <p className="join-midpoint-disclaimer">실제 소요시간은 운행 상황에 따라 다를 수 있어요.</p>
              </>
            )
          )}

          {isShowingHomeCandidates && midpointWithHome && midpointWithHome.status === 'READY' && (
            midpointWithHome.candidates.length === 0 ? (
              <div className="join-midpoint-empty">
                왕복 기준으로는 150분 안에 모두가 갈 수 있는 역을 찾지 못했어요.
              </div>
            ) : (
              <>
                <div className="join-midpoint-list">
                  {midpointWithHome.candidates.map((candidate, index) => (
                    <div key={candidate.stationName} className="join-midpoint-card">
                      <div className="join-midpoint-header">
                        <span className="join-midpoint-rank">{index + 1}위</span>
                        <span className="join-midpoint-name">{candidate.stationName}</span>
                        <span className="join-midpoint-lines">{renderLineBadges(candidate.stationName)}</span>
                        <span className="join-midpoint-fairness">공평도 {candidate.fairnessScore}</span>
                      </div>
                      <div className="join-midpoint-summary">
                        제일 오래 걸리는 사람{' '}
                        {Math.max(...candidate.times.map((time) => time.departureMinutes + time.homeMinutes))}분
                      </div>
                      <div className="join-midpoint-times">
                        {candidate.times.map((time) => (
                          <span
                            key={time.participantId}
                            className={
                              time.participantId === me.id
                                ? 'join-midpoint-time-chip join-midpoint-time-chip--me'
                                : 'join-midpoint-time-chip'
                            }
                          >
                            <span className="join-midpoint-time-chip-name">{time.nickname}</span>
                            <span className="join-midpoint-time-chip-value">
                              출발 {time.departureMinutes}분 · 귀가 {time.homeMinutes}분
                            </span>
                          </span>
                        ))}
                      </div>
                    </div>
                  ))}
                </div>
                <p className="join-midpoint-disclaimer">실제 소요시간은 운행 상황에 따라 다를 수 있어요.</p>
              </>
            )
          )}

          {room.owner
            && ((!isShowingHomeCandidates && midpoint.candidates.length > 0)
              || (isShowingHomeCandidates && midpointWithHome?.status === 'READY' && midpointWithHome.candidates.length > 0)) && (
            <button
              type="button"
              className="join-vote-start-btn"
              disabled={isStartingVoting}
              onClick={handleStartVoting}
            >
              {isStartingVoting ? '투표 시작하는 중...' : '이 후보들로 투표 시작'}
            </button>
          )}
        </>
      )}

      {isHomeStationModalOpen && (
        <Modal title="귀가역 설정" onClose={() => setIsHomeStationModalOpen(false)}>
          <StationAutocompleteInput
            value={homeStationModalInput}
            onChange={setHomeStationModalInput}
            stationOptions={stationOptions}
          />
          {homeStationModalError && <div className="join-error">{homeStationModalError}</div>}
          <button
            type="button"
            className="join-submit-cta"
            disabled={!homeStationModalInput.trim() || isSavingHomeStationModal}
            onClick={handleSaveHomeStationModal}
          >
            {isSavingHomeStationModal ? '저장하는 중...' : '저장'}
          </button>
        </Modal>
      )}

      {me && room.status === 'VOTING' && voteResult && (
        <>
          {room.owner && (
            <div className="join-vote-owner-row">
              <span className="join-progress-hint">후보가 고정됐어요. 체크가 모이면 1등을 확인해요.</span>
              <button
                type="button"
                className="join-vote-cancel-btn"
                disabled={isCancelingVoting}
                onClick={handleCancelVoting}
              >
                {isCancelingVoting ? '취소하는 중...' : '투표 취소'}
              </button>
            </div>
          )}
          {voteError && <div className="join-error">{voteError}</div>}
          <div className="join-midpoint-list">
            {voteResult.candidates.map((candidate) => (
              <div
                key={candidate.id}
                className={
                  candidate.id === voteResult.winnerCandidateId
                    ? 'join-midpoint-card join-vote-card--winner'
                    : 'join-midpoint-card'
                }
              >
                <div className="join-midpoint-header">
                  <span className="join-midpoint-rank">{candidate.rank}위</span>
                  <span className="join-midpoint-name">{candidate.stationName}</span>
                  <span className="join-midpoint-lines">{renderLineBadges(candidate.stationName)}</span>
                  {room.fairnessType !== 'SUM_MIN' && (
                    <span className="join-midpoint-fairness">공평도 {candidate.fairnessScore}</span>
                  )}
                </div>
                <div className="join-midpoint-summary">
                  {room.fairnessType === 'SUM_MIN'
                    ? `다 같이 총 ${candidate.sumMinutes}분`
                    : `제일 오래 걸리는 사람 ${candidate.maxMinutes}분`}
                </div>
                <div className="join-midpoint-times">
                  {candidate.times.map((time) => (
                    <span
                      key={time.participantId}
                      className={
                        me && time.participantId === me.id
                          ? 'join-midpoint-time-chip join-midpoint-time-chip--me'
                          : 'join-midpoint-time-chip'
                      }
                    >
                      <span className="join-midpoint-time-chip-name">{time.nickname}</span>
                      <span className="join-midpoint-time-chip-value">
                        {candidate.considerHome
                          ? `출발 ${time.departureMinutes}분 · 귀가 ${time.homeMinutes}분`
                          : `${time.minutes}분`}
                      </span>
                    </span>
                  ))}
                </div>
                <div className="join-vote-actions">
                  <button
                    type="button"
                    aria-pressed={candidate.votedByMe}
                    aria-label={candidate.votedByMe ? '좋아요 취소' : '좋아요'}
                    className="join-vote-check-btn"
                    disabled={pendingVoteIds.has(candidate.id)}
                    onClick={() => handleToggleVote(candidate.id, candidate.votedByMe)}
                  >
                    <svg
                      width="18"
                      height="18"
                      viewBox="0 0 24 24"
                      fill={candidate.votedByMe ? 'currentColor' : 'none'}
                      stroke="currentColor"
                      strokeWidth="2"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      aria-hidden="true"
                    >
                      <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
                    </svg>
                    <span>{candidate.voteCount}</span>
                  </button>
                  {room.owner && (
                    <button
                      type="button"
                      className="join-vote-confirm-btn"
                      onClick={() => setConfirmTarget(candidate)}
                    >
                      이 장소로 확정
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
          <p className="join-midpoint-disclaimer">
            {voteResult.winnerCandidateId
              ? '가장 많이 체크된 곳이에요.'
              : '체크 수가 같으면 아직 1등이 없어요.'}
          </p>
        </>
      )}

      {confirmTarget && (
        <ConfirmModal
          title="장소 확정"
          message={`${confirmTarget.stationName}(으)로 확정할까요?`}
          confirmLabel="확정"
          cancelLabel="취소"
          onConfirm={handleConfirmCandidate}
          onCancel={() => setConfirmTarget(null)}
        />
      )}

      {deleteModal}
    </div>
  )
}

export default JoinPage
