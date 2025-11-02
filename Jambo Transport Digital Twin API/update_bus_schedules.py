import csv
from datetime import datetime, timedelta

def parse_time(time_str):
    """Parse time string to datetime object"""
    parts = time_str.split(':')
    hours = int(parts[0])
    minutes = int(parts[1])
    seconds = int(parts[2])
    # Handle times >= 24:00:00
    if hours >= 24:
        hours = hours % 24
    return datetime.strptime(f"{hours:02d}:{minutes:02d}:{seconds:02d}", "%H:%M:%S")

def format_time(dt):
    """Format datetime to time string"""
    return dt.strftime("%H:%M:%S")

def update_stop_times():
    """Update stop times to stagger buses at different intervals"""

    # Read existing stop times
    with open('gtfs/stop_times.txt', 'r', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        fieldnames = reader.fieldnames
        stop_times = list(reader)

    # Time offsets for each bus (in minutes)
    # BUS1: 0 min, BUS2: 5 min, BUS3: 10 min, BUS4: 15 min
    bus_offsets = {
        'BUS1': 0,
        'BUS2': 5,
        'BUS3': 10,
        'BUS4': 15
    }

    updated_stop_times = []

    for stop_time in stop_times:
        trip_id = stop_time['trip_id']

        # Extract bus number from trip_id (e.g., ROUTE_A_BUS1_TRIP001 -> BUS1)
        if 'BUS1' in trip_id:
            bus_num = 'BUS1'
        elif 'BUS2' in trip_id:
            bus_num = 'BUS2'
        else:
            # Keep as is if not BUS1 or BUS2
            updated_stop_times.append(stop_time)
            continue

        # Get the offset for this bus
        offset_minutes = bus_offsets.get(bus_num, 0)

        # Parse and adjust arrival time
        arrival_dt = parse_time(stop_time['arrival_time'])
        arrival_dt += timedelta(minutes=offset_minutes)

        # Parse and adjust departure time
        departure_dt = parse_time(stop_time['departure_time'])
        departure_dt += timedelta(minutes=offset_minutes)

        # Update the times
        stop_time['arrival_time'] = format_time(arrival_dt)
        stop_time['departure_time'] = format_time(departure_dt)

        updated_stop_times.append(stop_time)

    # Add BUS3 and BUS4 schedules (duplicating BUS1 and BUS2 with different offsets)
    bus3_stop_times = []
    bus4_stop_times = []

    for stop_time in stop_times:
        trip_id = stop_time['trip_id']

        if 'BUS1' in trip_id:
            # Create BUS3 based on BUS1
            bus3_stop_time = stop_time.copy()
            bus3_trip_id = trip_id.replace('BUS1', 'BUS3')
            bus3_stop_time['trip_id'] = bus3_trip_id

            # Adjust times with BUS3 offset
            arrival_dt = parse_time(stop_time['arrival_time'])
            arrival_dt += timedelta(minutes=bus_offsets['BUS3'])

            departure_dt = parse_time(stop_time['departure_time'])
            departure_dt += timedelta(minutes=bus_offsets['BUS3'])

            bus3_stop_time['arrival_time'] = format_time(arrival_dt)
            bus3_stop_time['departure_time'] = format_time(departure_dt)

            bus3_stop_times.append(bus3_stop_time)

        elif 'BUS2' in trip_id:
            # Create BUS4 based on BUS2
            bus4_stop_time = stop_time.copy()
            bus4_trip_id = trip_id.replace('BUS2', 'BUS4')
            bus4_stop_time['trip_id'] = bus4_trip_id

            # Adjust times with BUS4 offset
            arrival_dt = parse_time(stop_time['arrival_time'])
            arrival_dt += timedelta(minutes=bus_offsets['BUS4'])

            departure_dt = parse_time(stop_time['departure_time'])
            departure_dt += timedelta(minutes=bus_offsets['BUS4'])

            bus4_stop_time['arrival_time'] = format_time(arrival_dt)
            bus4_stop_time['departure_time'] = format_time(departure_dt)

            bus4_stop_times.append(bus4_stop_time)

    # Combine all stop times
    all_stop_times = updated_stop_times + bus3_stop_times + bus4_stop_times

    # Write updated stop times
    with open('gtfs/stop_times.txt', 'w', encoding='utf-8', newline='') as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(all_stop_times)

    print(f"Updated stop_times.txt with {len(all_stop_times)} entries")
    print(f"  - Original entries: {len(stop_times)}")
    print(f"  - BUS3 entries added: {len(bus3_stop_times)}")
    print(f"  - BUS4 entries added: {len(bus4_stop_times)}")

def update_trips():
    """Update trips to include BUS3 and BUS4"""

    # Read existing trips
    with open('gtfs/trips.txt', 'r', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        fieldnames = reader.fieldnames
        trips = list(reader)

    new_trips = []

    for trip in trips:
        trip_id = trip['trip_id']

        if 'BUS1' in trip_id:
            # Create BUS3 based on BUS1
            bus3_trip = trip.copy()
            bus3_trip['trip_id'] = trip_id.replace('BUS1', 'BUS3')
            new_trips.append(bus3_trip)

        elif 'BUS2' in trip_id:
            # Create BUS4 based on BUS2
            bus4_trip = trip.copy()
            bus4_trip['trip_id'] = trip_id.replace('BUS2', 'BUS4')
            new_trips.append(bus4_trip)

    # Combine all trips
    all_trips = trips + new_trips

    # Write updated trips
    with open('gtfs/trips.txt', 'w', encoding='utf-8', newline='') as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(all_trips)

    print(f"\nUpdated trips.txt with {len(all_trips)} entries")
    print(f"  - Original trips: {len(trips)}")
    print(f"  - New trips added: {len(new_trips)}")

if __name__ == '__main__':
    print("Updating GTFS schedules...")
    print("\nBus start time offsets:")
    print("  - BUS1: 0 minutes (starts at 06:00:00)")
    print("  - BUS2: 5 minutes (starts at 06:05:00)")
    print("  - BUS3: 10 minutes (starts at 06:10:00)")
    print("  - BUS4: 15 minutes (starts at 06:15:00)")
    print()

    update_trips()
    update_stop_times()

    print("\nDone! Restart the application to load the new schedules.")
