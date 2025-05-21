import asyncio  # 비동기 처리를 위한 asyncio 모듈
import oracledb  # Oracle 데이터베이스 연결을 위한 모듈
from playwright.async_api import async_playwright  # 비동기 웹 자동화를 위한 Playwright 모듈

# Oracle 클라이언트 초기화 (Windows 기준 Oracle Instant Client 위치)
oracledb.init_oracle_client(lib_dir="C:/Oracle/instantclient_21_18")

# 크롤링할 항공편 경로와 DB 테이블 정보를 저장한 리스트
routes = [
    {"url": "https://kr.trip.com/flights/showfarefirst?dcity=sel&acity=osa&ddate=2025-05-26&rdate=2025-05-29&triptype=ow&class=y&lowpricesource=searchform&quantity=1&searchboxarg=t&nonstoponly=off&locale=ko-KR&curr=KRW",
     "table": "tripcom_flights_KIX"},
    {"url": "https://kr.trip.com/flights/showfarefirst?dcity=sel&acity=hkg&ddate=2025-05-26&rdate=2025-05-29&triptype=ow&class=y&lowpricesource=searchform&quantity=1&searchboxarg=t&nonstoponly=off&locale=ko-KR&curr=KRW",
     "table": "tripcom_flights_HKG"},
    {"url": "https://kr.trip.com/flights/showfarefirst?dcity=sel&acity=sin&ddate=2025-05-26&rdate=2025-05-29&triptype=ow&class=y&lowpricesource=searchform&quantity=1&searchboxarg=t&nonstoponly=off&locale=ko-KR&curr=KRW",
     "table": "tripcom_flights_SIN"}
]

async def scrape_and_save(url, table_name):
    flight_data = []  # 크롤링한 데이터를 저장할 리스트

    async with async_playwright() as p:  # 비동기 Playwright 컨텍스트 관리
        browser = await p.chromium.launch(headless=True)  # 헤드리스 모드로 브라우저 실행
        page = await browser.new_page()  # 새 페이지 열기
        await page.goto(url, timeout=60000)  # URL 방문, 타임아웃 60초 설정
        await page.wait_for_selector(".f-info-head.is-v2.u-clearfix.result-item-dep.selected")  # 로딩 대기

        previous_height = 0  # 이전 높이 초기화
        for _ in range(10):  # 최대 10회 스크롤
            await page.mouse.wheel(0, 3000)  # 스크롤 수행
            await page.wait_for_timeout(2000)  # 스크롤 후 2초 대기
            current_height = await page.evaluate("document.body.scrollHeight")  # 현재 높이
            if current_height == previous_height:  # 높이가 같으면 종료
                break
            previous_height = current_height

        # 크롤링할 항목 선택
        cards = await page.locator(".f-info-head.is-v2.u-clearfix.result-item-dep.selected").all()

        # 각 카드에서 데이터 추출
        for card in cards:
            try:
                airline = await card.locator("[data-testid='flights-name']").inner_text()
                depart_time = await card.locator("[data-testid^='flight-time']").nth(0).inner_text()
                arrive_time = await card.locator("[data-testid^='flight-time']").nth(1).inner_text()
                flight_time = f"{depart_time} → {arrive_time}"
                departure_code = await card.locator(".is-departure_2a2b .flight-info-stop__code_e162").inner_text()
                arrival_code = await card.locator(".is-arrival_f407 .flight-info-stop__code_e162").inner_text()
                price_text = await card.locator("[data-testid='u_price_info']").inner_text()
                price = int(price_text.replace(",", "").replace("원", "").replace("₩", "").strip())

                # 추출된 데이터 저장
                flight_data.append({
                    "airline": airline,
                    "flight_time": flight_time,
                    "price": price,
                    "departure": departure_code.split()[0],
                    "arrival": arrival_code.split()[0]
                })

            except Exception as e:
                print(f"⚠️ 파싱 실패: {e}")

        await browser.close()  # 브라우저 닫기

    print(f"✈ {table_name} 수집된 항공편 수: {len(flight_data)}")

    # Oracle DB 연결 설정
    dsn = oracledb.makedsn("localhost", 39161, service_name="XE")
    conn = oracledb.connect(user="CrawlingPJ", password="CrawlingPJ", dsn=dsn)
    cursor = conn.cursor()

    # 데이터베이스에 데이터 삽입
    for flight in flight_data:
        cursor.execute(f"""
            INSERT INTO {table_name}
            (id, airline, flight_time, price, flight_number, departure, arrival)
            VALUES ({table_name}_seq.NEXTVAL, :1, :2, :3, NULL, :4, :5)
        """, (
            flight["airline"],
            flight["flight_time"],
            flight["price"],
            flight["departure"],
            flight["arrival"]
        ))

    conn.commit()  # 변경사항 DB에 반영
    cursor.close()  # 커서 닫기
    conn.close()  # 연결 닫기
    print(f"✅ {table_name} 저장 완료!")

async def main():
    # 모든 경로에 대해 크롤링 및 저장 수행
    for route in routes:
        await scrape_and_save(route["url"], route["table"])

# 메인 비동기 함수 실행
asyncio.run(main())
