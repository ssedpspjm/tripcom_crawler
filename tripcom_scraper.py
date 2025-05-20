import asyncio
import oracledb
from playwright.async_api import async_playwright

# ✅ Oracle Instant Client 설정
oracledb.init_oracle_client(lib_dir="C:/Oracle/instantclient_21_18")

# ✅ 링크 & 저장할 테이블 매핑
routes = [
    {
        "url": "https://kr.trip.com/flights/showfarefirst?dcity=sel&acity=osa&ddate=2025-05-26&rdate=2025-05-29&triptype=ow&class=y&lowpricesource=searchform&quantity=1&searchboxarg=t&nonstoponly=off&locale=ko-KR&curr=KRW",
        "table": "tripcom_flights_KIX"
    },
    {
        "url": "https://kr.trip.com/flights/showfarefirst?dcity=sel&acity=hkg&ddate=2025-05-26&rdate=2025-05-29&triptype=ow&class=y&lowpricesource=searchform&quantity=1&searchboxarg=t&nonstoponly=off&locale=ko-KR&curr=KRW",
        "table": "tripcom_flights_HKG"
    },
    {
        "url": "https://kr.trip.com/flights/showfarefirst?dcity=sel&acity=sin&ddate=2025-05-26&rdate=2025-05-29&triptype=ow&class=y&lowpricesource=searchform&quantity=1&searchboxarg=t&nonstoponly=off&locale=ko-KR&curr=KRW",
        "table": "tripcom_flights_SIN"
    }
]

# ✅ 크롤링 + DB 저장 함수
async def scrape_and_save(url, table_name):
    flight_data = []

    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=True)
        page = await browser.new_page()
        await page.goto(url, timeout=60000)
        await page.wait_for_selector(".f-info-head.is-v2.u-clearfix.result-item-dep.selected")

        # 스크롤 다운 여러 번 (더 많은 항공편 로딩)
        previous_height = 0
        for _ in range(10):
            await page.mouse.wheel(0, 3000)
            await page.wait_for_timeout(2000)
            current_height = await page.evaluate("document.body.scrollHeight")
            if current_height == previous_height:
                break
            previous_height = current_height

        cards = await page.locator(".f-info-head.is-v2.u-clearfix.result-item-dep.selected").all()

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

                flight_data.append({
                    "airline": airline,
                    "flight_time": flight_time,
                    "price": price,
                    "departure": departure_code.split()[0],
                    "arrival": arrival_code.split()[0]
                })

            except Exception as e:
                print(f"⚠️ 파싱 실패: {e}")

        await browser.close()

    print(f"✈ {table_name} 수집된 항공편 수: {len(flight_data)}")

    # ✅ Oracle DB 저장
    dsn = oracledb.makedsn("localhost", 39161, service_name="XE")
    conn = oracledb.connect(user="CrawlingPJ", password="CrawlingPJ", dsn=dsn)
    cursor = conn.cursor()

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

    conn.commit()
    cursor.close()
    conn.close()
    print(f"✅ {table_name} 저장 완료!")

# ✅ 전체 실행
async def main():
    for route in routes:
        await scrape_and_save(route["url"], route["table"])

asyncio.run(main())
