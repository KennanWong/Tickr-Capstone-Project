import { Box } from '@mui/system'
import React from 'react'
import ReplyCard from './ReplyCard'

export default function ReviewReplies({replies, review_id, isAttendee}){
  const [replies_list, setReplies] = React.useState(replies)

  React.useEffect(() => {
    setReplies(replies)
  }, [replies])

  return (
    <Box sx={{display: 'flex', flexDirection: 'column', gap: 2}}>
      {replies_list.map((reply, key) => {
        return (
          <ReplyCard key={key} reply_details={reply} reply_num={replies.length - 1 - key} review_id={review_id} isAttendee={isAttendee}/>
        )
      })}
    </Box>
  )
}