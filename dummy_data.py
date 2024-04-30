from time import sleep
import requests
import sys
import json
import base64

def toDataUrl (data, mimetype):
    return f"data:{mimetype};base64,{base64.b64encode(data).decode()}"

def makeDataUrl (picturePath):
    with open(picturePath, "rb") as f:
        filetype = picturePath.split(".")[-1].strip()
        if filetype != "png" and filetype != "jpg" and filetype != "jpeg":
            raise RuntimeError("Invalid mimetype: " + picturePath)

        if filetype == "jpg":
            filetype = "jpeg"
        return toDataUrl(f.read(), "image/" + filetype)

global backendUrl
backendUrl = None
def makeUser (username, firstName, lastName, password, email, dob, profilePic, profile):
    response = requests.post(backendUrl + "/api/user/register", json={
        "user_name" : username,
        "first_name" : firstName,
        "last_name" : lastName,
        "password" : password,
        "email" : email,
        "date_of_birth" : dob
    })
    response.raise_for_status()

    authToken = response.json()["auth_token"]

    response = requests.get(backendUrl + "/api/user/search", params={
        "email" : email
    })
    response.raise_for_status()

    userId = response.json()["user_id"]

    profilePicData = None
    if profilePic:
        profilePicData = makeDataUrl(profilePic)

    if profilePicData or profile:
        response = requests.put(backendUrl + "/api/user/editprofile", json={
            "auth_token" : authToken,
            "user_name" : username,
            "first_name" : firstName,
            "last_name" : lastName,
            "email" : email,
            "profile_picture" : profilePicData,
            "profile_description" : profile if profile else ""
        })
        response.raise_for_status()

    return (authToken, userId)

def makeEvent (authToken, eventName, picturePath, details, admins, published):
    print(details["spotify_playlist"])
    request = {
        "auth_token" : authToken,
        "event_name" : eventName,
        "location" : details["location"],
        "start_date" : details["start_date"],
        "end_date" : details["end_date"],
        "description" : details["description"],
        "seating_details" : details["seating_details"],
        "categories" : details["categories"],
        "tags" : details["tags"],
        "admins" : admins,
        "spotify_playlist" : details["spotify_playlist"]
    }
    if picturePath:
        request["picture"] = makeDataUrl(picturePath)
    #print(request)
    response = requests.post(backendUrl + "/api/test/event/create", json=request)
    response.raise_for_status()

    eventId =  response.json()["event_id"]

    if published:
        response = requests.put(backendUrl + "/api/event/edit", json = {
            "auth_token" : authToken,
            "event_id" : eventId,
            "published" : True,
            "spotify_playlist" : details["spotify_playlist"]
        })
        response.raise_for_status()

    return eventId

def purchaseTicket (authToken, eventId, ticketTime, ticketDetails, notifications):
    response = requests.post(backendUrl + "/api/ticket/reserve", json={
        "auth_token" : authToken,
        "event_id" : eventId,
        "ticket_datetime" : ticketTime,
        "ticket_details" : ticketDetails
    })

    response.raise_for_status()

    reserveIds = [i["reserve_id"] for i in response.json()["reserve_tickets"]]

    response = requests.post(backendUrl + "/api/ticket/purchase", json={
        "auth_token" : authToken,
        "ticket_details" : [{"request_id" : i} for i in reserveIds],
        "success_url" : "http://example.com",
        "cancel_url" : "http://example.com"
    })
    response.raise_for_status()

    response = requests.post(backendUrl + "/api/event/notifications/update", json={
        "auth_token" : authToken,
        "event_id" : eventId,
        "notifications" : notifications
    })

    return len(reserveIds)

def makeReview (authToken, eventId, title, text, rating):
    response = requests.post(backendUrl + "/api/event/review/create", json={
        "auth_token" : authToken,
        "event_id" : eventId,
        "title" : title,
        "text" : text,
        "rating" : rating
    })

    response.raise_for_status()

    return response.json()["review_id"]

def makeReply (authToken, reviewId, reply):
    response = requests.post(backendUrl + "/api/event/review/reply", json={
        "auth_token" : authToken,
        "review_id" : reviewId,
        "reply" : reply
    })

    response.raise_for_status()

    return response.json()["reply_id"]

def makeReact (authToken, commentId, reactType):
    response = requests.post(backendUrl + "/api/event/review/react", json={
        "auth_token" : authToken,
        "comment_id" : commentId,
        "react_type" : reactType
    })

    response.raise_for_status()

def generateData (dataPath):
    data = {}
    with open(dataPath) as f:
        data = json.load(f)
    
    print("Clearing existing data!")
    response = requests.delete(backendUrl + "/api/test/clear", json={})
    response.raise_for_status()
    '''
    Users:
    {
        "user_name" : str,
        "first_name" : str,
        "last_name" : str,
        "password" : str,
        "email" : str,
        "date_of_birth" : str
    }
    '''

    users = {}
    for i in data["users"]:
        users[i["email"]] = makeUser(i["user_name"], i["first_name"], i["last_name"], i["password"], i["email"], i["date_of_birth"], i.get("picture", None), i.get("profile", None))
        print(f"Made user {i['email']} as {users[i['email']][1]}!")

    '''
    Events:
    {
        "host" : str (email),
        "id" : str,
        "picture" : str (path),
        "event_name" : str
        "location" : location,
        "start_date" : datetime,
        "end_date" : datetime,
        "description" : str,
        "seating_details" : seating details,
        "categories" : [str],
        "tags" : [str],
        "admins" : [str (email)]
        "published" : bool,
        "spotify_playlist" : str
    }
    '''
    events = {}
    for i in data["events"]:
        events[i["id"]] = makeEvent(users[i["host"]][0], i["event_name"], i["picture"] if "picture" in i else None, {
            "location" : i["location"],
            "start_date" : i["start_date"],
            "end_date" : i["end_date"],
            "description" : i["description"],
            "seating_details" : i["seating_details"],
            "categories" : i["categories"],
            "tags" : i["tags"],
            "spotify_playlist" : i.get("spotify_playlist", None)
        }, [users[j][1] for j in i["admins"]], i["published"])
        print(f"Made event {i['id']} for {i['host']} with id {events[i['id']]}")

    '''
    Tickets
    {
        "event" : str (event id),
        "user" : str (email),
        "ticket_datetime" : datetime
        "ticket_details" : [
            {
                "section" : str,
                "quantity" : int,
                "seat_numbers" : [int]
            }
        ]
    }
    '''
    for i in data["tickets"]:
        n = purchaseTicket(users[i["user"]][0], events[i["event"]], i["ticket_datetime"], i["ticket_details"], i.get("notif", True))
        print(f"Made {n} tickets for user {i['user']} for event {i['event']}")
    #sleep(0.5)
    '''
    Reviews
    {
        "user" : str (email),
        "event" : str (id),
        "id" : str,
        "title" : str,
        "text" : str,
        "rating" : float
    }
    '''

    reviews = {}
    for i in data["reviews"]:
        reviews[i["id"]] = makeReview(users[i["user"]][0], events[i["event"]], i["title"], i["text"], i["rating"])
        print(f"Made review {i['id']} for user {i['user']} in event {i['event']}")
    
    '''
    Replies
    {
        "user" : str (email),
        "review" : str (id),
        "id" : str,
        "reply" : str
    }
    '''
    replies = {}
    for i in data["replies"]:
        replies[i['id']] = makeReply(users[i["user"]][0], reviews[i["review"]], i["reply"])
        print(f"Made reply {i['id']} for user {i['user']} to review {i['review']}")
    
    '''
    Reactions
    {
        "user" : str (email),
        "comment" : str (id),
        "react_type" : str
    }
    '''
    for i in data["reacts"]:
        if i["comment"] in reviews:
            makeReact(users[i["user"]][0], reviews[i["comment"]], i["react_type"])
        else:
            makeReact(users[i["user"]][0], replies[i["comment"]], i["react_type"])
        
        print(f"Made react {i['react_type']} for user {i['user']} on comment {i['comment']}")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Required: data.json")
        sys.exit(1)
    
    backendUrl = "http://localhost:8080"
    generateData(sys.argv[1])