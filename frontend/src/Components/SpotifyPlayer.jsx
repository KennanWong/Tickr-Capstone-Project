/*
  Spotify embded component courtesy of Spotify
*/

import { Typography } from '@mui/material';
import { Box } from '@mui/system';
import React from 'react'
import { alpha } from '@mui/system';

const CLIENT_ID = '7fe8e700da1947a88047f156715ab612';
const CLIENT_SECRET = 'a508ed7ca9cc45529d3f62e805b3d34a'

export default function SpotifyPlayer({link, editable=false}) {

  const url = new URL(link);
  const pathname = url.pathname
  return (
    <>
      {editable
        ? <Typography sx={{textAlign: 'center', pt: 0, color: '#999999'}}>If playlist does not load. Playlist may not exist</Typography>
        : <></>
      }
      <iframe
        id="spotify-frame"
        width={'100%'}
        height={'100%'}
        frameBorder={0}
        title="Spotify Web Player"
        src={`https://open.spotify.com/embed${pathname}?utm_source=generator`}
        style={{
          borderRadius: 8
        }}
        allow="clipboard-write; encrypted-media; fullscreen; picture-in-picture" 
        loading="lazy"
      />
    </>
  )
}